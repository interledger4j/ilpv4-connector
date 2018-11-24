package com.sappenin.interledger.ilpv4.connector.model.settings;

import org.immutables.value.Value;

import java.time.Duration;

/**
 * Defines route-update settings, either for the node globally, or for an account on a node. The default setting for a
 * node is to disable routing (in other words, generally only connectors do routing).
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface RouteBroadcastSettings {

  /**
   * Whether to broadcast known routes to the remote peer.
   */
  @Value.Default
  default boolean isSendRoutes() {
    return false;
  }

  /**
   * Whether to receive routing updates from the remote peer.
   */
  @Value.Default
  default boolean isReceiveRoutes() {
    return false;
  }

  /**
   * Frequency at which the connector broadcasts its routes to adjacent connectors, starting at the end of the previous
   * route-update operation.
   */
  @Value.Default
  default Duration getRouteBroadcastInterval() {
    return Duration.ofMillis(30000);
  }

  /**
   * The frequency at which the connector checks for expired routes.
   */
  @Value.Default
  default Duration getRouteCleanupInterval() {
    return Duration.ofMillis(1000);
  }

  /**
   * The maximum age of a route provided by this connector.
   */
  @Value.Default
  default Duration getRouteExpiry() {
    return Duration.ofMillis(45000);
  }

  /**
   * The maximum number of epochs to transmit with any particular routing update message sent via CCP.
   */
  @Value.Default
  default int getMaxEpochsPerRoutingTable() {
    return 50;
  }

}