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
@JsonPropertyOrder( {"createdAt", "modifiedAt", "prefix", "accountId"} )
public interface StaticRoute {

  static ImmutableStaticRoute.Builder builder() {
    return ImmutableStaticRoute.builder();
  }

  /**
   * A target address prefix that corresponds to {@code #getPeerAddress}.
   *
   * @return
   */
  InterledgerAddressPrefix addressPrefix();

  /**
   * The ILP address of the peer this route should route through.
   *
   * @return
   */
  AccountId accountId();

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
  @JsonPropertyOrder( {"createdAt", "modifiedAt", "prefix", "accountId"} )
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
    public abstract InterledgerAddressPrefix addressPrefix();

    @Override
    public abstract AccountId accountId();

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      StaticRoute staticRoute = (StaticRoute) o;

      return this.addressPrefix().equals(staticRoute.addressPrefix());
    }

    @Override
    public int hashCode() {
      return this.addressPrefix().hashCode();
    }
  }

}
