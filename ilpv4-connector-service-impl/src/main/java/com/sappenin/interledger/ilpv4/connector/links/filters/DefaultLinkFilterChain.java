package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.linkfilter.LinkFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class DefaultPacketSwitchFilterChain implements PacketSwitchFilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPacketSwitchFilterChain.class);

  private final List<PacketSwitchFilter> packetSwitchFilters;
  private final List<LinkFilter> linkFilters;
  private final Link link;

  // The index of the packet-switch filter to call next...
  private int _packetSwitchFilterIndex;

  // The index of the link-filter to call next...
  private int _linkFilterIndex;

  /**
   * A chain of filters that are applied to a routePacket request before forwarding the actual request to the link.
   *
   * @param packetSwitchFilters
   * @param link
   */
  public DefaultPacketSwitchFilterChain(
    final List<PacketSwitchFilter> packetSwitchFilters, final List<LinkFilter> linkFilters, final Link link
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.link = Objects.requireNonNull(link);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePreparePacket
  ) {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);

    if (this._packetSwitchFilterIndex < this.packetSwitchFilters.size()) {
      return packetSwitchFilters.get(_packetSwitchFilterIndex++).doFilter(sourceAccountId, sourcePreparePacket, this);
    } else {
      LOGGER.debug(
        "Starting Outbound Link Filter Chain. sourceAccountId: `{}` link={} packet={}",
        sourceAccountId, link, sourcePreparePacket
      );
      if (this._linkFilterIndex < this.linkFilters.size()) {
        return linkFilters.get(_linkFilterIndex++).doFilter(sourcePreparePacket, this);
      } else {
        LOGGER.debug(
          "Sending outbound ILP Prepare. sourceAccountId: `{}` link={} packet={}",
          sourceAccountId, link, sourcePreparePacket
        );
        return link.sendPacket(sourcePreparePacket);
      }
    }
  }
}
