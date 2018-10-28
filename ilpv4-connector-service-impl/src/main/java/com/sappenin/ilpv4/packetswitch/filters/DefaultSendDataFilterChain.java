package com.sappenin.ilpv4.packetswitch.filters;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.plugin.lpiv2.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DefaultSendDataFilterChain implements SendDataFilterChain {

  private final List<SendDataFilter> sendDataFilters;
  private final Plugin plugin;
  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a sendData request before forwarding the actual request to the plugin.
   *
   * @param sendDataFilters
   * @param plugin
   */
  public DefaultSendDataFilterChain(final List<SendDataFilter> sendDataFilters, final Plugin plugin) {
    this.sendDataFilters = Objects.requireNonNull(sendDataFilters);
    this.plugin = Objects.requireNonNull(plugin);
  }

  @Override
  public CompletableFuture<InterledgerFulfillPacket> doFilter(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException {

    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    if (this._filterIndex < this.sendDataFilters.size()) {
      return sendDataFilters.get(_filterIndex++).doFilter(sourceAccountAddress, sourcePreparePacket, this);
    } else {
      return plugin.sendData(sourcePreparePacket);
    }

  }
}
