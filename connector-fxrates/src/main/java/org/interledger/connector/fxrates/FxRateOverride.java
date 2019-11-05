package org.interledger.connector.fxrates;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.Nullable;

/**
 * Allows overriding of an exchange rate based on the asset codes being converted between.
 *
 * Design considerations to revisit:
 * - In an implementation involving a base rate, it's likely that a given override may be a combination of two
 *   overrides: one of the `from` to the `baseRate` and one from the `baseRate` to the `to`.
 * - It's _also_ a possibility that the ability to ignore the base rate is desirable and that a direct conversion
 *   going `from` to `to` is something we should support.
 */
public interface FxRateOverride {

  static ImmutableFxRateOverride.Builder builder() {
    return ImmutableFxRateOverride.builder();
  }

  /**
   *
   * @return the numeric id assigned by the database.
   */
  @Nullable
  Long id();

  /**
   *
   * @return the asset code we're converting from
   */
  String assetCodeFrom();

  /**
   *
   * @return the asset code we're converting to
   */
  String assetCodeTo();

  /**
   *
   * @return the rate to be applied to the from amount to compute the to amount
   */
  BigDecimal rate();

  /**
   * The date/time this FxRateOverride was created.
   *
   * @return An {@link Instant}.
   */
  default Instant createdAt() {
    return Instant.now();
  }

  /**
   * The date/time this FxRateOverride was last modified.
   *
   * @return An {@link Instant}.
   */
  default Instant modifiedAt() {
    return Instant.now();
  }

  // Purposefully not interned. Because we desire hashcode/equals to align with FxRateOverrideEntity, if this class
  // were to be interned, then constructing a new instance with the same id as an already interned instance
  // would simply return the old, immutable value, which would be incorrect.
  @Value.Immutable
  @Value.Modifiable
  @JsonSerialize(as = ImmutableFxRateOverride.class)
  @JsonDeserialize(as = ImmutableFxRateOverride.class)
  @JsonPropertyOrder( {"id", "createdAt", "modifiedAt", "assetCodeFrom", "assetCodeTo", "rate"})
  abstract class AbstractFxRateOverride implements FxRateOverride {

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
    public abstract String assetCodeFrom();

    @Override
    public abstract String assetCodeTo();

    @Override
    public abstract BigDecimal rate();

    private static String getNaturalId(FxRateOverride override) {
      return override.assetCodeFrom() + "-" + override.assetCodeTo();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      FxRateOverride fxRateOverride = (FxRateOverride) o;

      return getNaturalId(this).equals(getNaturalId(fxRateOverride));
    }

    @Override
    public int hashCode() {
      return getNaturalId(this).hashCode();
    }

  }

}
