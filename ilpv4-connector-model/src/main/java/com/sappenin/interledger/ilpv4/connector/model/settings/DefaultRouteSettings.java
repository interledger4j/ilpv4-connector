package com.sappenin.interledger.ilpv4.connector.model.settings;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;
import com.sappenin.interledger.ilpv4.connector.model.StaticRoute;

import java.util.List;
import java.util.Optional;

/**
 * Defines route settings for the connector itself, either globally, or on a per-account basis.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface DefaultRouteSettings {

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
  default boolean useParentForDefaultRoute() {
    return true;
  }

  /**
   * An optionally-defined account that should be used as the default route for all un-routed traffic. If empty, the
   * default route is disabled.
   */
  Optional<InterledgerAddress> getDefaultRoute();

  // TODO: add setting for something like: interledger protcols like Echo.

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
    if (!this.useParentForDefaultRoute()) {
      Preconditions.checkArgument(this.getDefaultRoute().isPresent());
    }
  }
}