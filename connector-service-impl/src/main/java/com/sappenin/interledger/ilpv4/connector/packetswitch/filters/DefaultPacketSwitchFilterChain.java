package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.links.NextHopInfo;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.DefaultLinkFilterChain;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
  // rely upon AccountSettings found in this cache. This design is preferable to using Hibernate's 2nd-level cache
  // because in-general, we don't want to Cache accountSettings to support clustered Connector environments (though
  // this may change in the future depending on benchmark results).
  private final Cache<AccountId, Optional<? extends AccountSettings>> accountSettingsCache;

  private final AccountSettingsRepository accountSettingsRepository;

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
    this(packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper, accountSettingsRepository,
      Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .maximumSize(5000)
        .build()
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
    final AccountSettingsRepository accountSettingsRepository,
    final Cache<AccountId, Optional<? extends AccountSettings>> accountSettingsCache
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = nextHopPacketMapper;
    this.accountSettingsRepository = accountSettingsRepository;
    this._filterIndex = 0;
    this.accountSettingsCache = Objects.requireNonNull(accountSettingsCache);
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
        sourceAccountSettings.getAccountId(), preparePacket
      );

      // Here, use the link-mapper to get the `next-hop`, create a LinkFilterChain, and then send.
      final NextHopInfo nextHopInfo = this.nextHopPacketMapper.getNextHopPacket(
        sourceAccountSettings.getAccountId(), preparePacket
      );

      final Link<? extends LinkSettings> link;
      if (nextHopInfo.nextHopAccountId().equals(PaymentRouter.PING_ACCOUNT_ID)) {
        link = this.linkManager.getPingLink();
      } else {
        link = this.linkManager.getOrCreateLink(nextHopInfo.nextHopAccountId());
      }

      logger.debug(
        "Sending outbound ILP Prepare: sourceAccountId: `{}` link={} packet={}",
        sourceAccountSettings.getAccountId(), link, preparePacket
      );

      final AccountSettings nextHopAccountSettings = accountSettingsCache
        .get(nextHopInfo.nextHopAccountId(), accountSettingsRepository::findByAccountId)
        .orElseThrow(() -> new AccountNotFoundProblem(nextHopInfo.nextHopAccountId()));

      // The final operation in the filter-chain is `link.sendPacket(newPreparePacket)`.
      return new DefaultLinkFilterChain(linkFilters, link)
        .doFilter(nextHopAccountSettings, nextHopInfo.nextHopPacket());
    }
  }
}
