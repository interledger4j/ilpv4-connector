package com.sappenin.interledger.ilpv4.connector.balances;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;

import java.util.concurrent.atomic.AtomicInteger;

@Value.Immutable
public interface AccountBalance {

  static ImmutableAccountBalance.Builder builder() {
    return ImmutableAccountBalance.builder();
  }

  /**
   * The {@link AccountId} for this account balance.
   *
   * @return
   */
  AccountId accountId();

  /**
   * The amount of units currently in this account balance.
   *
   * @return
   */
  AtomicInteger getAmount();

  /**
   * The settings associated with this account.
   *
   * @return
   */
  //AccountSettings getAccountSettings();

  /**
   * Currency code or other asset identifier that will be used to select the correct rate for this account.
   */
  String getAssetCode();

  /**
   * Interledger amounts are integers, but most currencies are typically represented as # fractional units, e.g. cents.
   * This property defines how many Interledger units make # up one regular unit. For dollars, this would usually be set
   * to 9, so that Interledger # amounts are expressed in nano-dollars.
   *
   * @return
   */
  int getAssetScale();
}
