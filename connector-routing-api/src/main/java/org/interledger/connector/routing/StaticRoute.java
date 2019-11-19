package org.interledger.connector.routing;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;

/**
 * A statically configured route. Static routes take precedence over the same or shorter prefixes that are local or
 * published by peers. More specific prefixes will still take precedence.
 */
@JsonSerialize(as = ImmutableStaticRoute.class)
@JsonDeserialize(as = ImmutableStaticRoute.class)
@JsonPropertyOrder( {"createdAt", "modifiedAt", "routePrefix", "nextHopAccountId"})
public interface StaticRoute {

  static ImmutableStaticRoute.Builder builder() {
    return ImmutableStaticRoute.builder();
  }

  /**
   * An Interledger address prefix is used to match against the destination address of a particular ILP packet. The
   * longest prefix in the routing table that matches the destination address of a particular packet will be used as the
   * best route for any particular packet.
   *
   * @return The {@link InterledgerAddressPrefix} for this static route.
   */
  InterledgerAddressPrefix routePrefix();

  /**
   * Identifies the account that outgoing packets should be forwarded upon in response to a routing operation.
   *
   * @return The {@link AccountId} of the route.
   */
  AccountId nextHopAccountId();

  /**
   * The date/time this StaticRoute was created.
   *
   * @return An {@link Instant}.
   */
  default Instant createdAt() {
    return Instant.now();
  }

  /**
   * The date/time this StaticRoute was last modified.
   *
   * @return An {@link Instant}.
   */
  default Instant modifiedAt() {
    return Instant.now();
  }

  // Purposefully not interned. Because we desire hashcode/equals to align with StaticRouteEntity, if this class
  // were to be interned, then constructing a new instance with the same id as an already interned instance
  // would simply return the old, immutable value, which would be incorrect.
  @Value.Immutable
  @JsonSerialize(as = ImmutableStaticRoute.class)
  @JsonDeserialize(as = ImmutableStaticRoute.class)
  @JsonPropertyOrder( {"createdAt", "modifiedAt", "routePrefix", "nextHopAccountId"})
  abstract class AbstractStaticRoute implements StaticRoute {

    @Override
    @Value.Default
    public Instant createdAt() {
      return Instant.now();
    }

    @Override
    @Value.Default
    public Instant modifiedAt() {
      return Instant.now();
    }

    @Override
    public abstract InterledgerAddressPrefix routePrefix();

    @Override
    public abstract AccountId nextHopAccountId();

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      StaticRoute staticRoute = (StaticRoute) o;

      // to match hibernate behavior
      return this.routePrefix().equals(staticRoute.routePrefix());
    }

    @Override
    public int hashCode() {
      return this.routePrefix().hashCode();
    }
  }

}
