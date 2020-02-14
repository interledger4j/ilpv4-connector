package org.interledger.connector.settings;

import org.interledger.connector.accounts.AccountId;

import org.immutables.value.Value;

import java.time.Duration;
import java.util.Optional;

/**
 * Defines connector-wide routing settings.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface GlobalRoutingSettings {

  static ImmutableGlobalRoutingSettings.Builder builder() {
    return ImmutableGlobalRoutingSettings.builder();
  }

  /**
   * Global setting that determines whether the connector broadcasts known routes. Defaults to {@code false}.
   */
  @Value.Default
  default boolean isRouteBroadcastEnabled() {
    return false;
  }

  /**
   * An optionally-defined account that should be used as the default route for all un-routed traffic. If empty, the
   * default route is disabled.
   */
  Optional<AccountId> defaultRoute();

  /**
   * Seed used for generating routing table auth values. If not specific, a random, ephemeral routing secret
   * will be used.
   */
  Optional<String> routingSecret();

  /**
   * An ILP address segment that will be used to route packets to any local accounts defined in the Connector. For
   * example, if an account exists in the Connector with id of `alice`, then nodes wanting to send packets to that
   * account would use the ILP address `{connector-operator-address}.accounts.alice`. Typically this will be used in
   * IL-DCP, but will also be used to make routing decisions for any local accounts that might connect to the Connector.
   * For example, `g.connector.accounts.alice.bob` would route to `alice`, allowing that node to figure out how to route
   * to "bob."
   *
   * @return A {@link String} that will be appended to this Connector's operating address in order to identify packets
   *   that should be routed to a local account defined in this Connector.
   */
  @Value.Default
  default String localAccountAddressSegment() {
    return "accounts";
  }

  /**
   * Determines if the first parent-account should be used as the default route. This value overrides any specified
   * `defaultRoute` value.
   */
  @Value.Default
  default boolean isUseParentForDefaultRoute() {
    return false;
  }

  /**
   * Frequency at which the connector broadcasts its routes to adjacent connectors, starting at the end of the previous
   * route-update operation.
   */
  @Value.Default
  default Duration routeBroadcastInterval() {
    return Duration.ofMillis(30000L);
  }

  /**
   * The frequency at which the connector checks for expired routes.
   */
  @Value.Default
  default Duration routeCleanupInterval() {
    return Duration.ofMillis(1000L);
  }

  /**
   * The maximum age of a route provided by this connector.
   */
  @Value.Default
  default Duration routeExpiry() {
    return Duration.ofMillis(45000L);
  }

  /**
   * The maximum number of epochs to transmit with any particular routing update message sent via CCP.
   */
  @Value.Default
  default int maxEpochsPerRoutingTable() {
    return 50;
  }

  @Value.Check
  default void verify() {
    // NOTE: It is acceptable to not have a default-route in certain scenarios. In these scenarios, if the route isn't
    // defined in the routing table, then the traffic just rejects (e.g., a tier1 Connector with no Parent likely
    // doesn't want a default route). Alternatively, we might want a DeadRoute that is registered by default in the
    // RoutingTable and sends traffic to a special handler that still rejects, but might also collect extra information.
  }
}
