package com.sappenin.interledger.ilpv4.connector.model.settings;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.util.Optional;

/**
 * <p>Defines how an account should maintain and enforce a balance. The balance is always from the perspective of the
 * node holding the account. Therefore, a negative balance implies the node owes value to the counterparty and a
 * positive balance implies the counterparty owes value to the node.</p>
 *
 * <p>The defaults on this object are configured very aggressively to essentially allow no money to travel through
 * the account, and to likewise settle all the time (i.e., very aggressively). Thus, by default, this settings object
 * will only support zero-value accounts.</p>
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface AccountBalanceSettings {

  /**
   * The minimum balance (in this account's indivisible base units) the connector must maintain. The connector will
   * reject outgoing packets if they would put it below this balance.
   *
   * @return The minimum balance, or {@link Optional#empty()} if there is no minimum.
   */
  @Value.Default
  default Optional<BigInteger> getMinBalance() {
    return Optional.of(BigInteger.ZERO);
  }

  /**
   * Maximum balance (in this account's indivisible base units) the connector will allow. The connector will reject
   * incoming packets if they would put it above this balance.
   *
   * @return The maximum balance, or {@link Optional#empty()} if there is no maximum.
   */
  @Value.Default
  default BigInteger getMaxBalance() {
    return BigInteger.ZERO;
  }

  /**
   * Optional Balance (in this account's indivisible base units) numerically below which the connector will
   * automatically initiate a settlement.
   *
   * @return The settlement threshold balance, or {@link Optional#empty()} if there is no threshold (i.e., the account
   * should never settle).
   */
  @Value.Default
  default Optional<BigInteger> getSettleThreshold() {
    return Optional.of(BigInteger.ZERO);
  }

  /**
   * Optional Balance (in this account's indivisible base units) the connector will attempt to reach when settling.
   *
   * @return
   */
  @Value.Default
  default Optional<BigInteger> getSettleTo() {
    return Optional.of(BigInteger.ZERO);
  }
}
