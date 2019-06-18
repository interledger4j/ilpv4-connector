package com.sappenin.interledger.ilpv4.connector.packetswitch;

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

  public DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final ConnectorExceptionHandler connectorExceptionHandler,
    final AccountSettingsRepository accountSettingsRepository,
    final PacketRejector packetRejector
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = Objects.requireNonNull(nextHopPacketMapper);
    this.connectorExceptionHandler = Objects.requireNonNull(connectorExceptionHandler);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.packetRejector = Objects.requireNonNull(packetRejector);
  }

  public final InterledgerResponsePacket switchPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingSourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingSourcePreparePacket);

    // TODO: Ensure this is cached!
    final AccountSettings sourceAccountSettings = accountSettingsRepository.findByAccountId(sourceAccountId)
      // REJECT due to no account...
      .orElseThrow(() -> new InterledgerProtocolException(
        packetRejector.reject(sourceAccountId, incomingSourcePreparePacket, InterledgerErrorCode.T00_INTERNAL_ERROR,
          String.format("No Account found: `%s`", sourceAccountId))));

    try {

      return new DefaultPacketSwitchFilterChain(packetSwitchFilters, linkFilters, linkManager, nextHopPacketMapper)
        .doFilter(sourceAccountSettings, incomingSourcePreparePacket);

    } catch (Exception e) {
      // Any rejections should be caught here, and returned as such....
      return this.connectorExceptionHandler.handleException(sourceAccountId, incomingSourcePreparePacket, e);
    }
  }
}