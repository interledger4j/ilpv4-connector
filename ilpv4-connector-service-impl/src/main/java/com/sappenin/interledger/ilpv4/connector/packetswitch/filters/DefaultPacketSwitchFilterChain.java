package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import com.sappenin.interledger.ilpv4.connector.links.NextHopInfo;
import com.sappenin.interledger.ilpv4.connector.links.NextHopPacketMapper;
import com.sappenin.interledger.ilpv4.connector.links.filters.DefaultLinkFilterChain;
import com.sappenin.interledger.ilpv4.connector.links.filters.LinkFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class DefaultPacketSwitchFilterChain implements PacketSwitchFilterChain {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final List<PacketSwitchFilter> packetSwitchFilters;

  // The outbound filter-chain that will be applied to the outgoing packet...
  private final List<LinkFilter> linkFilters;

  private final LinkManager linkManager;

  private final NextHopPacketMapper nextHopPacketMapper;

  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a switchPacket request before attempting to determine the `next-hop` {@link
   * Link} to forward the packet onto.
   */
  public DefaultPacketSwitchFilterChain(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = nextHopPacketMapper;
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId, final InterledgerPreparePacket preparePacket
  ) {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(preparePacket);

    if (this._filterIndex < this.packetSwitchFilters.size()) {
      return packetSwitchFilters.get(_filterIndex++).doFilter(sourceAccountId, preparePacket, this);
    } else {
      logger.debug("Sending outbound ILP Prepare: sourceAccountId: `{}` packet={}", sourceAccountId, preparePacket);

      // Here, use the link-mapper to get the `next-hop`, create a LinkFilterChain, and then send.
      final NextHopInfo nextHopInfo = this.nextHopPacketMapper.getNextHopPacket(sourceAccountId, preparePacket);
      final Link<? extends LinkSettings> link =
        this.linkManager.getOrCreateLink(nextHopInfo.nextHopAccountId());

      logger.debug(
        "Sending outbound ILP Prepare: sourceAccountId: `{}` link={} packet={}",
        sourceAccountId, link, preparePacket
      );

      // The final operation in the filter-chain is `link.sendPacket(newPreparePacket)`.
      return new DefaultLinkFilterChain(linkFilters, link)
        .doFilter(nextHopInfo.nextHopAccountId(), nextHopInfo.nextHopPacket());
    }
  }
}
