package org.interledger.connector.settings;

import org.interledger.connector.accounts.AccountId;

import org.immutables.value.Value;

/**
 * Indicates which connector features are enabled or disabled.
 */
public interface EnabledFeatureSettings {

  static ImmutableEnabledFeatureSettings.Builder builder() {
    return ImmutableEnabledFeatureSettings.builder();
  }

  /**
   * Whether this Connector rate-limits accounts.
   *
   * @return {@code true} if account-level rate limiting is enabled; {@code false} otherwise.
   */
  default boolean isRateLimitingEnabled() {
    return false;
  }

  /**
   * A configuration property that determines if the Connector supports connector-wide termination/fulfillment of
   * SPSP/STREAM packets. If enabled, any packets addressed to `g.{connector}.{spsp_prefix}.{connector_account}.{any}`
   * will be intercepted and forwarded to a local SPSP receiver Link for processing. The {@link AccountId} used will be
   * `{accountId}` as parsed from the ILP prepare packet's destination address (see template above).
   *
   * @return {@code true} if local SPSP filfillment should be attempted; {@code false} otherwise.
   */
  default boolean isLocalSpspFulfillmentEnabled() {
    return false;
  }

  /**
   * Flag to control if shared secrets must be 32 bytes
   *
   * @return true if required otherwise anything goes
   */
  default boolean isRequire32ByteSharedSecrets() {
    return true;
  }

  default PaymentTransactionMode paymentTransactionMode() {
    return PaymentTransactionMode.IN_MEMORY;
  }

  @Value.Immutable(intern = true)
  abstract class AbstractEnabledFeatureSettings implements EnabledFeatureSettings {

    @Override
    @Value.Default
    public boolean isRateLimitingEnabled() {
      return false;
    }

    @Override
    @Value.Default
    public boolean isLocalSpspFulfillmentEnabled() {
      return false;
    }

    @Override
    @Value.Default
    public boolean isRequire32ByteSharedSecrets() {
      return true;
    }

    @Override
    @Value.Default
    public PaymentTransactionMode paymentTransactionMode() {
      return PaymentTransactionMode.IN_MEMORY;
    }
  }

  enum PaymentTransactionMode {
    IN_MEMORY,
    IN_POSTGRES
  }

}
