package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DefaultPacketSwitchFilterChain implements PacketSwitchFilterChain {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final List<PacketSwitchFilter> packetSwitchFilters;
  private final Plugin plugin;
  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a routeData request before forwarding the actual request to the plugin.
   *
   * @param packetSwitchFilters
   * @param plugin
   */
  public DefaultPacketSwitchFilterChain(final List<PacketSwitchFilter> packetSwitchFilters, final Plugin plugin) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.plugin = Objects.requireNonNull(plugin);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> doFilter(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePreparePacket
  ) {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);

    if (this._filterIndex < this.packetSwitchFilters.size()) {
      return packetSwitchFilters.get(_filterIndex++).doFilter(sourceAccountId, sourcePreparePacket, this);
    } else {
      logger.debug(
        "Sending outbound ILP Prepare. sourceAccountId: `{}` plugin={} packet={}",
        sourceAccountId, plugin, sourcePreparePacket
      );
      return plugin.sendData(sourcePreparePacket);
    }
  }
}
