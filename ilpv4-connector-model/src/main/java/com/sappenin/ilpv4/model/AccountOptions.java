package com.sappenin.ilpv4.model;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

import javax.money.CurrencyUnit;
import java.math.BigInteger;

/**
 * Defines the attributes of an {@link Account}.
 */
@Value.Immutable
public interface AccountOptions {

  /**
   * The minimum balance this connector is allowed to have with the Counterparty of this account.
   */
  @Default
  default BigInteger getMinBalance() {
    return BigInteger.ZERO;
  }

  /**
   * The maximum balance this connector is allowed to have with the Counterparty of this account.
   */
  @Default
  default BigInteger getMaxBalance() {
    return BigInteger.ZERO;
  }

  /**
   * The currency unit of the asset underlying this account. (Despite the name of the returned object, this value may
   * represent a non-currency asset).
   *
   * @return A {@link CurrencyUnit}.
   */
  CurrencyUnit getAssetCode();

  /**
   * <p>The order of magnitude to express one full currency unit in this account's base units. More
   * formally, an integer (..., -2, -1, 0, 1, 2, ...), such that one of the account's base units equals
   * 10^-<tt>currencyScale</tt> <tt>currencyCode</tt></p> <p>For example, if the integer values represented on the
   * system are to be interpreted as dollar-cents (for the purpose of settling a user's account balance, for instance),
   * then the account's currencyScale is 2. The amount 10000 would be translated visually into a decimal format via the
   * following equation: 10000 * (10^(-2)), or "100.00".</p>
   */
  Integer getCurrencyScale();

  /**
   * The threshold over which this account must be settled before it can process more ILP payment packets.
   */
  BigInteger getSettleThreshold();

  /**
   * The maximum amount that a particular payment packet can contain.
   */
  BigInteger getMaximumPacketAmount();

  /**
   * The relationship between this account and the local node. When an Interledger node peers with another node through
   * an account, the source peer will establish a relationship that can have one of three types depending on how it fits
   * into the wider network hierarchy.
   *
   * @return An {@link AccountRelationship}.
   */
  AccountRelationship getRelationship();
}
