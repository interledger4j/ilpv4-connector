package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.events.PacketEventPublisher;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.DefaultLinkFilterChain;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerRuntimeException;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.PacketRejector;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A default implementation of {@link PacketSwitchFilterChain}.
 */
public class DefaultPacketSwitchFilterChain implements PacketSwitchFilterChain {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PacketRejector packetRejector;

  private final List<PacketSwitchFilter> packetSwitchFilters;

  // The outbound filter-chain that will be applied to the outgoing packet...
  private final List<LinkFilter> linkFilters;

  private final LocalDestinationAddressUtils localDestinationAddressUtils;

  private final LinkManager linkManager;

  private final NextHopPacketMapper nextHopPacketMapper;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache.
  private final AccountSettingsLoadingCache accountSettingsLoadingCache;

  // The index of the filter to call next...
  private int _filterIndex;

  private PacketEventPublisher packetEventPublisher;

  /**
   * A chain of filters that are applied to a switchPacket request before attempting to determine the `next-hop` {@link
   * Link} to forward the packet onto.
   *
   * @param packetRejector               A {@link PacketRejector} for rejecting packets in a uniform manner.
   * @param packetSwitchFilters          A {@link List} of type {@link PacketSwitchFilter}.
   * @param linkFilters                  A {@link List} of {@link LinkFilter}.
   * @param localDestinationAddressUtils A {@lnk LocalDestinationAddressUtils}.
   * @param linkManager                  A {@link LinkManager}.
   * @param nextHopPacketMapper          A {@link NextHopPacketMapper}.
   * @param accountSettingsLoadingCache  A {@link AccountSettingsLoadingCache}.
   * @param packetEventPublisher         A {@link PacketEventPublisher}.
   */
  public DefaultPacketSwitchFilterChain(
    final PacketRejector packetRejector,
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LocalDestinationAddressUtils localDestinationAddressUtils,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final AccountSettingsLoadingCache accountSettingsLoadingCache,
    final PacketEventPublisher packetEventPublisher
  ) {
    this.packetRejector = Objects.requireNonNull(packetRejector);
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.localDestinationAddressUtils = Objects.requireNonNull(localDestinationAddressUtils);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = Objects.requireNonNull(nextHopPacketMapper);
    this.packetEventPublisher = Objects.requireNonNull(packetEventPublisher);
    this.accountSettingsLoadingCache = Objects.requireNonNull(accountSettingsLoadingCache);
    this._filterIndex = 0;
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings, final InterledgerPreparePacket preparePacket
  ) {
    Objects.requireNonNull(sourceAccountSettings);
    Objects.requireNonNull(preparePacket);

    // This entire method MUST be wrapped in a broad try/catch to ensure that the filterChain is never aborted
    // accidentally. If an error is emitted anywhere in the filter-chain, it is always mapped to a reject packet so
    // that the entire filter-chain can always be processed properly.
    // See https://github.com/interledger4j/ilpv4-connector/issues/588
    try {

      if (this._filterIndex < this.packetSwitchFilters.size()) {
        // Apply all PacketSwitch filters...
        return packetSwitchFilters.get(_filterIndex++).doFilter(sourceAccountSettings, preparePacket, this);
      } else { // forwardPacket(sourceAccountSettings, preparePacket)

        // ...and then send the new packet to its destination on the correct outbound link.
        logger.debug(
          "Sending outbound ILP Prepare: sourceAccountId: `{}` packet={}",
          sourceAccountSettings.accountId(), preparePacket
        );

        // Here, use the link-mapper to get the `next-hop`, create a LinkFilterChain, and then send.
        final NextHopInfo nextHopInfo = this.nextHopPacketMapper.getNextHopPacket(
          sourceAccountSettings, preparePacket
        );

        final AccountSettings nextHopAccountSettings = accountSettingsLoadingCache
          .getAccount(nextHopInfo.nextHopAccountId())
          .orElseThrow(() -> new AccountNotFoundProblem(nextHopInfo.nextHopAccountId()));

        final Link<? extends LinkSettings> link = computeLink(nextHopAccountSettings, preparePacket.getDestination());

        // The final operation in the filter-chain is `link.sendPacket(newPreparePacket)`.
        InterledgerResponsePacket response = new DefaultLinkFilterChain(packetRejector, linkFilters, link)
          .doFilter(nextHopAccountSettings, nextHopInfo.nextHopPacket());

        /////////////
        // Packet Tracking
        this.trackPacket(sourceAccountSettings, preparePacket, nextHopInfo, nextHopAccountSettings, response);

        return response;
      }
    } catch (Exception e) {
      // If anything in the filterchain emits an exception, this is considered a failure case. These always translate
      // into a rejection.
      logger.error("Failure in PacketSwitchFilterChain: " + e.getMessage(), e);
      if (InterledgerRuntimeException.class.isAssignableFrom(e.getClass())) {
        return ((InterledgerProtocolException) e).getInterledgerRejectPacket();
      } else {
        return packetRejector.reject(
          LinkId.of(sourceAccountSettings.accountId().value()),
          preparePacket,
          InterledgerErrorCode.T00_INTERNAL_ERROR, e.getMessage()
        );
      }
    }
  }

  /**
   * Track this packet by emitting proper events depending on the response.
   */
  private void trackPacket(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket preparePacket,
    final NextHopInfo nextHopInfo,
    final AccountSettings nextHopAccountSettings,
    final InterledgerResponsePacket response
  ) {
    try {
      BigDecimal fxRate = nextHopPacketMapper.determineExchangeRate(
        sourceAccountSettings, nextHopAccountSettings, preparePacket
      );
      response.handle(interledgerFulfillPacket ->
        packetEventPublisher.publishFulfillment(
          sourceAccountSettings,
          nextHopAccountSettings,
          preparePacket,
          nextHopInfo.nextHopPacket(),
          fxRate,
          interledgerFulfillPacket.getFulfillment()
        ), (rejectPacket) ->
        packetEventPublisher.publishRejectionByNextHop(
          sourceAccountSettings,
          nextHopAccountSettings,
          preparePacket,
          nextHopInfo.nextHopPacket(),
          fxRate,
          rejectPacket
        )
      );
    } catch (Exception e) {
      logger.warn("Could not publish event", e);
    }
  }

  /**
   * In this section of code, the Packet Switch needs to determine the correct Link to use to "forward" a particular
   * packet. In the general case, the linkManager should simply be asked to return a Link for a given accountId (i.e.,
   * find the Link for nextHopInfo.nextHopAccountId()). However, sometimes it is desirable to locally fulfill a packet
   * on behalf of an account without actually forwarding the Packet "out" of the  Connector. One such example is when
   * the Connector has been directed to locally fulfill an SPSP packet,  In this case, the Connector needs to utilize a
   * different Link that doesn't exactly correspond simply to  the nextHop's accountId. Instead, the Link obtained
   * depends on the structure of the ILP address. For  example, a Connector operating with the ILP address `g.connector`
   * would typically fulfill SPSP addresses  like `g.connector.spsp.alice.123xyz`. In this case, the Packet Switch
   * should process this packet using Alice's  account, but should use the SpspReceiverLink instead of the typical Link
   * assigned in Alice's AccountSettings.
   *
   * @param nextHopAccountSettings
   * @param destinationAddress
   *
   * @return
   */
  @VisibleForTesting
  Link computeLink(final AccountSettings nextHopAccountSettings, final InterledgerAddress destinationAddress) {
    if (localDestinationAddressUtils.isLocalSpspDestinationAddress(destinationAddress)) {
      return this.linkManager.getOrCreateSpspReceiverLink(nextHopAccountSettings);
    } else {
      return this.linkManager.getOrCreateLink(nextHopAccountSettings);
    }
  }
}
