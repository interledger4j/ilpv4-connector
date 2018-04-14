package com.sappenin.ilpv4.model;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.util.UUID;

/**
 * An account tracks a balance between two Interledger peers.
 */
@Value.Immutable
public interface Account {

  /**
   * An local identifier for the account
   *
   * @return a UUID identifying the account
   */
  AccountId getAccountId();

  /**
   * The current balance of this account.
   *
   * @return A {@link BigInteger} representing the balance of this account.
   */
  BigInteger getBalance();

  /**
   * All configurable options related to this account.
   */
  AccountOptions getAccountOptions();

}
