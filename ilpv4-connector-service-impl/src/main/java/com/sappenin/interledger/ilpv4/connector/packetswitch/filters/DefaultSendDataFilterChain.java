package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.plugin.lpiv2.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  public CompletableFuture<Optional<InterledgerResponsePacket>> doFilter(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePreparePacket
  ) {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);

    if (this._filterIndex < this.sendDataFilters.size()) {
      return sendDataFilters.get(_filterIndex++).doFilter(sourceAccountId, sourcePreparePacket, this);
    } else {
      return plugin.sendData(sourcePreparePacket);
    }

  }
}
