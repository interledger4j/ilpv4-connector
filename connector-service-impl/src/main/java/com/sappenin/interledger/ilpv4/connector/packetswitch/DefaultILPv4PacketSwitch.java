package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.ConnectorExceptionHandler;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.DefaultPacketSwitchFilterChain;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A default implementation of {@link ILPv4PacketSwitch}.
 */
public class DefaultILPv4PacketSwitch implements ILPv4PacketSwitch {

  private final List<PacketSwitchFilter> packetSwitchFilters;
  private final List<LinkFilter> linkFilters;
  private final LinkManager linkManager;
  private final NextHopPacketMapper nextHopPacketMapper;
  private final ConnectorExceptionHandler connectorExceptionHandler;
  private final PacketRejector packetRejector;
  private final AccountSettingsRepository accountSettingsRepository;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache. This design is preferable to using Hibernate's 2nd-level cache
  // because in-general, we don't want to Cache accountSettings to support clustered Connector environments (though
  // this may change in the future depending on benchmark results).
  private final Cache<AccountId, Optional<? extends AccountSettings>> accountSettingsCache;

  /**
   * For testing purposes.
   */
  @VisibleForTesting
  protected DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final ConnectorExceptionHandler connectorExceptionHandler,
    final AccountSettingsRepository accountSettingsRepository,
    final PacketRejector packetRejector
  ) {
    this(
      packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper, connectorExceptionHandler, packetRejector,
      accountSettingsRepository, Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .maximumSize(5000)
        .build(accountSettingsRepository::findByAccountId)
    );
  }

  public DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final ConnectorExceptionHandler connectorExceptionHandler,
    final PacketRejector packetRejector,
    final AccountSettingsRepository accountSettingsRepository,
    final Cache<AccountId, Optional<? extends AccountSettings>> accountSettingsCache
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = Objects.requireNonNull(nextHopPacketMapper);
    this.connectorExceptionHandler = Objects.requireNonNull(connectorExceptionHandler);
    this.packetRejector = Objects.requireNonNull(packetRejector);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.accountSettingsCache = Objects.requireNonNull(accountSettingsCache);
  }

  /**
   * Perform the logic of packet switching by first applying one or more instance of {@link PacketSwitchFilter}, and
   * then once an appropriate outbound link has been chosen, apply one or more instances of {@link LinkFilter} and then
   * send the {@code incomingSourcePreparePacket} out on the chosen outbound link.
   *
   * @param sourceAccountId             An {@link AccountId} for the account that received the {@code
   *                                    incomingSourcePreparePacket}.
   * @param incomingSourcePreparePacket The packet received from the inbound/source account.
   *
   * @return An {@link InterledgerResponsePacket} as received from the outbound link.
   */
  public final InterledgerResponsePacket switchPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingSourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingSourcePreparePacket);

    return this.accountSettingsCache.get(sourceAccountId, accountSettingsRepository::findByAccountId)
      .map(accountSettingsEntity -> {
        try {
          return new DefaultPacketSwitchFilterChain(
            packetSwitchFilters,
            linkFilters,
            linkManager,
            nextHopPacketMapper,
            accountSettingsRepository,
            accountSettingsCache
          ).doFilter(accountSettingsEntity, incomingSourcePreparePacket);

        } catch (Exception e) {
          // Any rejections should be caught here, and returned as such....
          return this.connectorExceptionHandler.handleException(sourceAccountId, incomingSourcePreparePacket, e);
        }
      })
      .orElseThrow(() -> {
        // REJECT due to no account...
        return new InterledgerProtocolException(
          packetRejector.reject(sourceAccountId, incomingSourcePreparePacket, InterledgerErrorCode.T00_INTERNAL_ERROR,
            String.format("No Account found: `%s`", sourceAccountId)));
      });
  }
}
