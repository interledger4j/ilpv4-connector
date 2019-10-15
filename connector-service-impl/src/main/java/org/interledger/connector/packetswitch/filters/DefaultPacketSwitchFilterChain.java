package org.interledger.connector.packetswitch.filters;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.DefaultLinkFilterChain;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final LinkManager linkManager;

  private final NextHopPacketMapper nextHopPacketMapper;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache.
  private final AccountSettingsLoadingCache accountSettingsLoadingCache;

  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * For testing purposes only.
   */
  @VisibleForTesting
  protected DefaultPacketSwitchFilterChain(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final AccountSettingsRepository accountSettingsRepository
  ) {
    this(
      packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper,
      new AccountSettingsLoadingCache(accountSettingsRepository)
    );
  }

  /**
   * A chain of filters that are applied to a switchPacket request before attempting to determine the `next-hop` {@link
   * Link} to forward the packet onto.
   */
  public DefaultPacketSwitchFilterChain(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final AccountSettingsLoadingCache accountSettingsLoadingCache
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = nextHopPacketMapper;
    this._filterIndex = 0;
    this.accountSettingsLoadingCache = Objects.requireNonNull(accountSettingsLoadingCache);
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

      final Link<? extends LinkSettings> link;
      if (nextHopInfo.nextHopAccountId().equals(PaymentRouter.PING_ACCOUNT_ID)) {
        link = this.linkManager.getPingLink();
      } else {
        link = this.linkManager.getOrCreateLink(nextHopInfo.nextHopAccountId());
      }

      logger.debug(
        "Sending outbound ILP Prepare: sourceAccountId: `{}` link={} packet={}",
        sourceAccountSettings.accountId(), link, preparePacket
      );

      final AccountSettings nextHopAccountSettings = accountSettingsLoadingCache
        .getAccount(nextHopInfo.nextHopAccountId())
        .orElseThrow(() -> new AccountNotFoundProblem(nextHopInfo.nextHopAccountId()));

      // The final operation in the filter-chain is `link.sendPacket(newPreparePacket)`.
      return new DefaultLinkFilterChain(linkFilters, link)
        .doFilter(nextHopAccountSettings, nextHopInfo.nextHopPacket());
    }
  }
}
