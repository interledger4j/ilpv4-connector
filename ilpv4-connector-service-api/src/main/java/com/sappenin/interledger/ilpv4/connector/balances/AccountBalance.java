package com.sappenin.interledger.ilpv4.connector.balances;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountSettings;

import java.math.BigInteger;

@Value.Immutable
public interface Balance {

  /**
   * The amount of units currently in this account balance.
   *
   * @return
   */
  BigInteger getAmount();

  /**
   * The settings associated with this account.
   *
   * @return
   */
  AccountSettings getAccountSettings();
}
