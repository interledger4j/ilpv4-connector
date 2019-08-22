package com.sappenin.interledger.ilpv4.connector.routing;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>An entry in a {@link RoutingTable}, used by Interledger nodes to determine the "next hop" account that a payment
 * should be forwarded to in order to complete a payment.</p>
 *
 * <p> For more details about the structure of this class as it relates to other routes in a routing table, reference
 * {@link RoutingTable}.</p>
 */
public interface SourceRestrictedRoute extends Route {

  String ALLOW_ALL_SOURCES = "(.*?)";

  /**
   * <p>A regular expression that can restrict routing table destinations to a subset of allowed payment-source
   * prefixes. By default, this filter allows all sources.</p>
   *
   * @return A {@link Pattern}
   */
  default Pattern getSourcePrefixRestrictionRegex() {
    // Default to allow all sources.
    return Pattern.compile(ALLOW_ALL_SOURCES);
  }

  /**
   * An abstract implementation of {@link SourceRestrictedRoute} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractSourceRestrictedRoute implements SourceRestrictedRoute {

    @Default
    @Override
    public Pattern getSourcePrefixRestrictionRegex() {
      // Default to allow all sources.
      return Pattern.compile(ALLOW_ALL_SOURCES);
    }

    // These are overridden because Pattern does not define an equals method, which default to Object equality, making
    // no pattern ever equal to any other pattern.

    /**
     * This instance is equal to all instances of {@code SourceRestrictedRoute} that have equal attribute values.
     *
     * @return {@code true} if {@code this} is equal to {@code another} instance
     */
    @Override
    public boolean equals(Object another) {
      if (this == another) {
        return true;
      }
      return another instanceof AbstractSourceRestrictedRoute
        && equalTo((AbstractSourceRestrictedRoute) another);
    }

    // TODO: Use rely on parent for seeding the equalTo and hashCode.
    private boolean equalTo(AbstractSourceRestrictedRoute another) {
      return getSourcePrefixRestrictionRegex().pattern().equals(another.getSourcePrefixRestrictionRegex().pattern())
        && Arrays.equals(getAuth(), another.getAuth())
        && getRoutePrefix().equals(another.getRoutePrefix())
        && getNextHopAccountId().equals(another.getNextHopAccountId())
        && getPath().equals(another.getPath())
        && Objects.equals(getExpiresAt(), another.getExpiresAt());
    }

    /**
     * Computes a hash code from attributes: {@code sourcePrefixRestrictionRegex}, {@code auth}, {@code targetPrefix},
     * {@code nextHopAccount}, {@code path}, {@code expiresAt}.
     *
     * @return hashCode value
     */
    @Override
    public int hashCode() {
      int h = 5381;
      h += (h << 5) + getSourcePrefixRestrictionRegex().pattern().hashCode();
      h += (h << 5) + Arrays.hashCode(getAuth());
      h += (h << 5) + getRoutePrefix().hashCode();
      h += (h << 5) + getNextHopAccountId().hashCode();
      h += (h << 5) + getPath().hashCode();
      h += (h << 5) + Objects.hashCode(getExpiresAt());
      return h;
    }

  }
}
