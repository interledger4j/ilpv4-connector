package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;

import java.util.List;
import java.util.Objects;
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
  private final AccountSettingsRepository accountSettingsRepository;
  private final PacketRejector packetRejector;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache. This design is preferable to using Hibernate's 2nd-level cache
  // because in-general, we don't want to Cache accountSettings to support clustered Connector environments (though
  // this may change in the future depending on benchmark results).
  private final LoadingCache<AccountId, AccountSettingsEntity> accountSettingsCache;

  public DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final ConnectorExceptionHandler connectorExceptionHandler,
    final AccountSettingsRepository accountSettingsRepository,
    final PacketRejector packetRejector
  ) {
    this(packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper, connectorExceptionHandler,
      accountSettingsRepository, packetRejector,
      Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES) // TODO Make this duration configurable
        .refreshAfterWrite(20, TimeUnit.SECONDS) // TODO Make this duration configurable
        .maximumSize(5000) // TODO: Make size configurable.
        .build(sourceAccountId -> accountSettingsRepository.findByAccountId(sourceAccountId).orElse(null))
    );
  }

  /**
   * For testing purposes.
   */
  DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final ConnectorExceptionHandler connectorExceptionHandler,
    final AccountSettingsRepository accountSettingsRepository,
    final PacketRejector packetRejector,
    final LoadingCache<AccountId, AccountSettingsEntity> accountSettingsCache
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = Objects.requireNonNull(nextHopPacketMapper);
    this.connectorExceptionHandler = Objects.requireNonNull(connectorExceptionHandler);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.packetRejector = Objects.requireNonNull(packetRejector);
    this.accountSettingsCache = Objects.requireNonNull(accountSettingsCache);
  }

  public final InterledgerResponsePacket switchPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingSourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingSourcePreparePacket);

    final AccountSettings sourceAccountSettings = this.accountSettingsCache.get(sourceAccountId);
    if (sourceAccountSettings == null) {
      // REJECT due to no account...
      throw new InterledgerProtocolException(
        packetRejector.reject(sourceAccountId, incomingSourcePreparePacket, InterledgerErrorCode.T00_INTERNAL_ERROR,
          String.format("No Account found: `%s`", sourceAccountId)));
    }

    try {
      return new DefaultPacketSwitchFilterChain(packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper)
        .doFilter(sourceAccountSettings, incomingSourcePreparePacket);

    } catch (Exception e) {
      // Any rejections should be caught here, and returned as such....
      return this.connectorExceptionHandler.handleException(sourceAccountId, incomingSourcePreparePacket, e);

    }
  }
}
