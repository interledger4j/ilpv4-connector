package org.interledger.connector.settings;

import com.google.common.collect.Lists;
import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.StaticRoute;

import java.time.Duration;
import java.util.List;
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
   *
   * @see {@link StaticRoute#STANDARD_DEFAULT_ROUTE}.
   */
  Optional<AccountId> defaultRoute();

  /**
   * Seed used for generating routing table auth values.
   */
  String routingSecret();

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

  /**
   * Contains any preconfigured static routes for this connector.
   *
   * @return An Collection of type {@link StaticRoute}.
   */
  @Value.Default
  default List<? extends StaticRoute> staticRoutes() {
    return Lists.newArrayList();
  }

  @Value.Check
  default void verify() {
    // This code is commented out because in some use-cases, it's acceptable to not have a default-route. In these
    // scenarios, if the route isn't defined in the routing table, then the traffic just rejects (e.g., a tier1
    // Connector with no Parent likely doesn't want a default route). Alternatively, we might want a DeadRoute that
    // is registered by default in the RoutingTable and sends traffic to a special handler that still rejects, but
    // might also collect extra information.

    // If we're not using the parent as a default route, then a default route must be specified!
    //    if (!this.isUseParentForDefaultRoute()) {
    //      Preconditions.checkArgument(
    //        this.defaultRoute().isPresent(),
    //        "A default ILP Route MUST be set if this ILSP is not relying upon a Parent Account for this information!"
    //      );
    //    }
  }
}
