package com.sappenin.ilpv4.model.settings;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Defines how a connector should maintain and enforce a balance for an account. The balance is always from the
 * connector's perspective. Therefore, a negative balance implies the connector owes money to the counterparty and a
 * positive balance implies the counterparty owes money to the connector.
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
  Optional<BigInteger> getMinBalance();

  /**
   * Maximum balance (in this account's indivisible base units) the connector will allow. The connector will reject
   * incoming packets if they would put it above this balance.
   *
   * @return The maximum balance, or {@link Optional#empty()} if there is no maximum.
   */
  BigInteger getMaxBalance();

  /**
   * Optional Balance (in this account's indivisible base units) numerically below which the connector will
   * automatically initiate a settlement.
   *
   * @return The settlement threshold balance, or {@link Optional#empty()} if there is no threshold (i.e., the account
   * should never settle).
   */
  Optional<BigInteger> getSettleThreshold();

  /**
   * Optional Balance (in this account's indivisible base units) the connector will attempt to reach when settling.
   *
   * @return
   */
  Optional<BigInteger> getSettleTo();
}
