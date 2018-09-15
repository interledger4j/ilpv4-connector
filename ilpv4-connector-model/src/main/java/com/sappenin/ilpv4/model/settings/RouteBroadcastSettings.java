package com.sappenin.ilpv4.model.settings;

import org.immutables.value.Value;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Defines route-update settings, either for the Connector globally, or for an account.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface RouteBroadcastSettings {

  /**
   * Whether to broadcast known routes to the remote peer.
   */
  @Value.Default
  default boolean isSendRoutes() {
    return true;
  }

  /**
   * Whether to receive routing updates from the remote peer.
   */
  @Value.Default
  default boolean isReceiveRoutes() {
    return true;
  }

  /**
   * Frequency at which the connector broadcasts its routes to adjacent connectors, starting at the end of the previous
   * route-update operation.
   */
  @Value.Default
  default Duration getRouteBroadcastInterval() {
    return Duration.of(30000, ChronoUnit.MILLIS);
  }

  /**
   * The frequency at which the connector checks for expired routes.
   */
  @Value.Default
  default Duration getRouteCleanupInterval() {
    return Duration.of(1000, ChronoUnit.MILLIS);
  }

  /**
   * The maximum age of a route provided by this connector.
   */
  @Value.Default
  default Duration getRouteExpiry() {
    return Duration.of(45000, ChronoUnit.MILLIS);
  }

  /**
   * The maximum number of epochs to transmit with any particular routing update message sent via CCP.
   */
  @Value.Default
  default int getMaxEpochsPerRoutingTable() {
    return 50;
  }

}