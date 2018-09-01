package com.sappenin.ilpv4.model;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import javax.money.CurrencyUnit;
import java.math.BigInteger;

// TODO: Remove most of this information into AccountSettings....Balance comes from the AccountManager, or directly
// from the Balance tracker, and settings comes from AccountSettings. Everything else is not needed....


/**
 * An account tracks a balance between two Interledger peers.
 *
 * @deprecated Will go away once all settings are configured properly (will be replaced by AccountSettings).
 */
@Deprecated
@Value.Immutable
public interface Account {

  /**
   * The ILP Address for this account.
   *
   * @return A {@link InterledgerAddress} identifying the account
   */
  InterledgerAddress getInterledgerAddress();

  /**
   * The type of plugin that this account uses to communicate with its peer.
   *
   * @return
   */
  PluginType getPluginType();

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

  // TODO: Move settings into AccountSettings, like min/max balance, etc.

  // TODO: Extract this to an AccountSettings object that is accessible via ConnectorSettings.

  /**
   * The minimum balance this connector is allowed to have with the Counterparty of this account.
   */
  // TODO: Make this optional instead of zero. A min-balance of zero might be valid.
  @Value.Default
  default BigInteger getMinBalance() {
    return BigInteger.ZERO;
  }

  /**
   * The maximum balance this connector is allowed to have with the Counterparty of this account.
   */
  // TODO: Make this optional instead of zero. A min-balance of zero might be valid.
  @Value.Default
  default BigInteger getMaxBalance() {
    return BigInteger.ZERO;
  }

  /**
   * The threshold over which this account must be settled before it can process more ILP payment packets.
   */
  @Value.Default
  // TODO: Make this optional instead of zero. A min-balance of zero might be valid.
  default BigInteger getSettleThreshold() {
    return BigInteger.valueOf(100);
  }

  /**
   * The maximum amount that a particular payment packet can contain.
   */
  @Value.Default
  // TODO: Make this optional instead of zero. A min-balance of zero might be valid.
  default BigInteger getMaximumPacketAmount() {
    return BigInteger.valueOf(100);
  }

}
