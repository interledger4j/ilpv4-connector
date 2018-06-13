package com.sappenin.ilpv4.model;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import javax.money.CurrencyUnit;
import java.math.BigInteger;

/**
 * An account tracks a balance between two Interledger peers.
 */
@Value.Immutable
public interface Account {

  /**
   * The ILP Address for this account.
   *
   * @return A {@link InterledgerAddress} identifying the account
   */
  // TODO: Convert to ILP Address once https://github.com/hyperledger/quilt/issues/139 is fixed to relax the ILP
  // address.
  String getInterledgerAddress();

  /**
   * The ILP Address for the Connector operating this account.
   *
   * @return
   */
  String getConnectorInterledgerAddress();

  /**
   * The current balance of this account.
   *
   * @return A {@link BigInteger} representing the balance of this account.
   */
  @Value.Default
  default BigInteger getBalance() {
    return BigInteger.ZERO;
  }

  /**
   * The type of plugin that this account uses to communicate with its peer.
   *
   * @return
   */
  Plugin getPlugin();

  /**
   * The minimum balance this connector is allowed to have with the Counterparty of this account.
   */
  @Value.Default
  default BigInteger getMinBalance() {
    return BigInteger.ZERO;
  }

  /**
   * The maximum balance this connector is allowed to have with the Counterparty of this account.
   */
  @Value.Default
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
  @Value.Default
  default BigInteger getSettleThreshold() {
    return BigInteger.valueOf(100);
  }

  /**
   * The maximum amount that a particular payment packet can contain.
   */
  @Value.Default
  default BigInteger getMaximumPacketAmount() {
    return BigInteger.valueOf(100);
  }

}
