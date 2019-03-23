package org.interledger.connector.accounts;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * <p>Defines the maximum rate of incoming packets for this account, using a permit-issuing system. The rate-limit is
 * defined primarily by the rate at which permits are issued. Absent additional configuration, permits will be
 * distributed at a fixed rate, defined in terms of permits per second. Permits will be distributed smoothly, with the
 * delay between individual permits being adjusted to ensure that the configured rate is maintained.</p>
 *
 * <p>It is possible to configure a RateLimiter to have a warmup period during which time the permits issued each
 * second steadily increases until it hits the stable rate.</p>
 */
public interface AccountRateLimitSettings {

  static ImmutableAccountRateLimitSettings.Builder builder() {
    return ImmutableAccountRateLimitSettings.builder();
  }

  /**
   * The maximum number of packets-per-second that is allowed for this account. If this value is not specified, then no
   * limit is applied to this account.
   *
   * @return The maximum number of packets-per-second that is allowed for this account.
   */
  Optional<Integer> getMaxPacketsPerSecond();

  @Value.Immutable(intern = true)
  @Value.Modifiable
  abstract class AbstractAccountRateLimitSettings implements AccountRateLimitSettings {

  }
}
