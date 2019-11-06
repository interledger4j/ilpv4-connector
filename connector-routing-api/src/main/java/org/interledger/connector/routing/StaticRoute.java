package org.interledger.connector.routing;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;
import javax.annotation.Nullable;

public interface StaticRoute {

  static ImmutableStaticRoute.Builder builder() {
    return ImmutableStaticRoute.builder();
  }

  @Nullable
  Long id();

  InterledgerAddressPrefix prefix();

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
  @Value.Modifiable
  @JsonSerialize(as = ImmutableStaticRoute.class)
  @JsonDeserialize(as = ImmutableStaticRoute.class)
  @JsonPropertyOrder( {"id", "createdAt", "modifiedAt", "prefix", "accountId"} )
  abstract class AbstractStaticRoute implements StaticRoute {

    @Override
    @Nullable
    public abstract Long id();

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
    public abstract InterledgerAddressPrefix prefix();

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

      return this.prefix().equals(staticRoute.prefix());
    }

    @Override
    public int hashCode() {
      return this.prefix().hashCode();
    }
  }

}
