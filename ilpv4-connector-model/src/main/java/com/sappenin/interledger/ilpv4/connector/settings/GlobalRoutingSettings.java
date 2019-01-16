package com.sappenin.interledger.ilpv4.connector.settings;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.StaticRoute;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Defines connector-wide routing settings.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface GlobalRoutingSettings {

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
  Optional<AccountId> getDefaultRoute();

  /**
   * Seed used for generating routing table auth values.
   */
  @Value.Default
  default String getRoutingSecret() {
    // TODO: Need to generate 32 bytes of random data for auth, once it's used.
    return "";
  }

  /**
   * Determines if the first parent-account should be used as the default route. This value overrides any specified
   * `defaultRoute` value.
   */
  @Value.Default
  default boolean isUseParentForDefaultRoute() {
    return true;
  }

  /**
   * Frequency at which the connector broadcasts its routes to adjacent connectors, starting at the end of the previous
   * route-update operation.
   */
  @Value.Default
  default Duration getRouteBroadcastInterval() {
    return Duration.ofMillis(30000L);
  }

  /**
   * The frequency at which the connector checks for expired routes.
   */
  @Value.Default
  default Duration getRouteCleanupInterval() {
    return Duration.ofMillis(1000L);
  }

  /**
   * The maximum age of a route provided by this connector.
   */
  @Value.Default
  default Duration getRouteExpiry() {
    return Duration.ofMillis(45000L);
  }

  /**
   * The maximum number of epochs to transmit with any particular routing update message sent via CCP.
   */
  @Value.Default
  default int getMaxEpochsPerRoutingTable() {
    return 50;
  }

  /**
   * Contains any preconfigured static routes for this connector.
   *
   * @return An Collection of type {@link StaticRoute}.
   */
  @Value.Default
  default List<? extends StaticRoute> getStaticRoutes() {
    return Lists.newArrayList();
  }

  @Value.Check
  default void verify() {
    // If we're not using the parent as a default route, then a default route must be specified!
    if (!this.isUseParentForDefaultRoute()) {
      Preconditions.checkArgument(this.getDefaultRoute().isPresent());
    }
  }
}