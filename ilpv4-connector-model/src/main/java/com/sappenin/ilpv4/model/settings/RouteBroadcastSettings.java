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
   * Whether to broadcast known routes.
   */
  @Value.Default
  default boolean routeBroadcastEnabled() {
    return true;
  }

  /**
   * Frequency at which the connector broadcasts its routes to adjacent connectors, starting at the end of the previous
   * route-update operation.
   */
  @Value.Default
  default Duration routeBroadcastInterval() {
    return Duration.of(30000, ChronoUnit.MILLIS);
  }

  /**
   * The frequency at which the connector checks for expired routes.
   */
  @Value.Default
  default Duration routeCleanupInterval() {
    return Duration.of(1000, ChronoUnit.MILLIS);
  }

  /**
   * The maximum age of a route provided by this connector.
   */
  @Value.Default
  default Duration routeExpiry() {
    return Duration.of(45000, ChronoUnit.MILLIS);
  }

  /**
   * Seed used for generating routing table auth values.
   */
  @Value.Default
  default String routingSecret() {
    return "";
  }

  /**
   * The maximum number of epochs to transmit with any particular routing update message sent via CCP.
   */
  @Value.Default
  default long maxEpochsPerRoutingTable() {
    return 50;
  }

}