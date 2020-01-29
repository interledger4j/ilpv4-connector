package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.sub.SpspSubAccountUtils;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.DefaultLinkFilterChain;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;

import com.google.common.eventbus.EventBus;
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

  private final List<PacketSwitchFilter> packetSwitchFilters;

  // The outbound filter-chain that will be applied to the outgoing packet...
  private final List<LinkFilter> linkFilters;

  private final SpspSubAccountUtils subAccountUtils;

  private final LinkManager linkManager;

  private final NextHopPacketMapper nextHopPacketMapper;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache.
  private final AccountSettingsLoadingCache accountSettingsLoadingCache;

  private final EventBus eventBus;

  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a switchPacket request before attempting to determine the `next-hop` {@link
   * Link} to forward the packet onto.
   *
   * @param packetSwitchFilters
   * @param linkFilters
   * @param subAccountUtils
   * @param linkManager
   * @param nextHopPacketMapper
   * @param accountSettingsLoadingCache
   * @param eventBus
   */
  public DefaultPacketSwitchFilterChain(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final SpspSubAccountUtils subAccountUtils,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final AccountSettingsLoadingCache accountSettingsLoadingCache,
    final EventBus eventBus
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.subAccountUtils = Objects.requireNonNull(subAccountUtils);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = nextHopPacketMapper;
    this.eventBus = eventBus;
    this.accountSettingsLoadingCache = Objects.requireNonNull(accountSettingsLoadingCache);
    this._filterIndex = 0;
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings, final InterledgerPreparePacket preparePacket
  ) {
    Objects.requireNonNull(sourceAccountSettings);
    Objects.requireNonNull(preparePacket);

    if (this._filterIndex < this.packetSwitchFilters.size()) {
      // Apply all PacketSwitch filters...
      return packetSwitchFilters.get(_filterIndex++).doFilter(sourceAccountSettings, preparePacket, this);
    } else {
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

      // In this section of code, the Packet Switch needs to determine the correct Link to use to "forward" a
      // particular packet. In the general case, the linkManager should simply be asked to return a Link for a given
      // accountId (i.e., find the Link for nextHopInfo.nextHopAccountId()). However, sometimes it is desirable to
      // locally fulfill a packet on behalf of an account without actually forwarding the Packet "out" of the
      // Connector. One such example is when the Connector has been directed to locally fulfill an SPSP packet,
      // In this case, the Connector needs to utilize a different Link that doesn't exactly correspond simply to
      // the nextHop's accountId. Instead, the Link obtained depends on the structure of the ILP address. For
      // example, a Connector operating with the ILP address `g.connector` would typically fulfill SPSP addresses
      // like `g.connector.alice.123xyz`. In this case, Packet Switch should process this packet in Alice's account,
      // but should use the SpspReceiverLink instead of the typical Link assigned in Alice's AccountSettings.
      final Link<? extends LinkSettings> link;
      if (subAccountUtils.isConnectorPingAccountId(nextHopInfo.nextHopAccountId())) {
        // TODO: Remove this overt ping-link check once the LinkManager is smart enough to not create a new Link on
        //  _every_ call per https://github.com/interledger4j/ilpv4-connector/issues/535
        // link = this.linkManager.getOrCreateLink(subAccountUtils.getConnectorPingAccountId());
        link = this.linkManager.getPingLink();
      } else if (subAccountUtils.shouldFulfilLocally(preparePacket.getDestination())) {
        link = this.linkManager.getOrCreateSpspReceiverLink(nextHopAccountSettings);
      } else {
        link = this.linkManager.getOrCreateLink(nextHopAccountSettings);
      }

      logger.debug(
        "Sending outbound ILP Prepare: sourceAccountId: `{}` link={} packet={}",
        sourceAccountSettings.accountId(), link, preparePacket
      );

      // The final operation in the filter-chain is `link.sendPacket(newPreparePacket)`.
      InterledgerResponsePacket response = new DefaultLinkFilterChain(linkFilters, link)
        .doFilter(nextHopAccountSettings, nextHopInfo.nextHopPacket());

      BigDecimal fxRate = nextHopPacketMapper.determineExchangeRate(sourceAccountSettings, nextHopAccountSettings,
        preparePacket);

      try {
        response.handle(interledgerFulfillPacket ->
          eventBus.post(PacketFulfillmentEvent.builder()
            .accountSettings(sourceAccountSettings)
            .destinationAccount(nextHopAccountSettings)
            .exchangeRate(fxRate)
            .incomingPreparePacket(preparePacket)
            .outgoingPreparePacket(nextHopInfo.nextHopPacket())
            .fulfillment(interledgerFulfillPacket.getFulfillment())
            .message("response packet for " + preparePacket.getExecutionCondition())
            .build()
          ), (rejectPacket) -> {
        });
      } catch (Exception e) {
        logger.warn("Could not publish event", e);
      }
      return response;
    }
  }
}
