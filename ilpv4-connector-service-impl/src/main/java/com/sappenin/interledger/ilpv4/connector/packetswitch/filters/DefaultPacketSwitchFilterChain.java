package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DefaultPacketSwitchFilterChain implements PacketSwitchFilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPacketSwitchFilterChain.class);

  private final List<PacketSwitchFilter> packetSwitchFilters;
  private final Link link;
  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a routeData request before forwarding the actual request to the link.
   *
   * @param packetSwitchFilters
   * @param link
   */
  public DefaultPacketSwitchFilterChain(final List<PacketSwitchFilter> packetSwitchFilters, final Link link) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.link = Objects.requireNonNull(link);
  }

  @Override
  public Optional<InterledgerResponsePacket> doFilter(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePreparePacket
  ) {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);

    if (this._filterIndex < this.packetSwitchFilters.size()) {
      return packetSwitchFilters.get(_filterIndex++).doFilter(sourceAccountId, sourcePreparePacket, this);
    } else {
      LOGGER.debug(
        "Sending outbound ILP Prepare. sourceAccountId: `{}` link={} packet={}",
        sourceAccountId, link, sourcePreparePacket
      );
      return link.sendPacket(sourcePreparePacket);
    }
  }
}
