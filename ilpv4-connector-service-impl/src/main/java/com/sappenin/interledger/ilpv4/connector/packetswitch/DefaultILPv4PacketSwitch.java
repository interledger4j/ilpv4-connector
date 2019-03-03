package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.DefaultPacketSwitchFilterChain;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * A default implementation of {@link ILPv4PacketSwitch}.
 */
public class DefaultILPv4PacketSwitch implements ILPv4PacketSwitch {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final List<PacketSwitchFilter> packetSwitchFilters;
  private final List<LinkFilter> linkFilters;
  private final AccountManager accountManager;
  private final NextHopPacketMapper nextHopPacketMapper;


  public DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final AccountManager accountManager,
    final NextHopPacketMapper nextHopPacketMapper
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.nextHopPacketMapper = Objects.requireNonNull(nextHopPacketMapper);
  }

  public final InterledgerResponsePacket switchPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingSourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingSourcePreparePacket);

    try {
      return new DefaultPacketSwitchFilterChain(packetSwitchFilters, linkFilters, accountManager, nextHopPacketMapper)
        .doFilter(sourceAccountId, incomingSourcePreparePacket);
    } catch (InterledgerProtocolException e) {
      // Any rejections should be caught here, and returned as such....
      return e.getInterledgerRejectPacket();
    }
  }
}