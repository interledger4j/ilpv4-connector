package com.sappenin.interledger.ilpv4.connector.balances;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks the details of a balance change.
 */
@Value.Immutable
public interface BalanceChangeResult {

  static ImmutableBalanceChangeResult.Builder builder() {
    return ImmutableBalanceChangeResult.builder();
  }

  UUID balanceTransferId();

  /**
   * The {@link AccountId} for the balance that was mutated..
   *
   * @return
   */
  AccountId accountId();

  /**
   * The amount of units currently in this account balance.
   *
   * @return
   */
  BigInteger amount();

  /**
   * The previous balance of the account, before applying this change.
   *
   * @return
   */
  Optional<BigInteger> accountPreviousBalance();

  /**
   * The new balance of the account after this change is applied (but only if the change was applied).
   *
   * @return
   */
  Optional<BigInteger> accountNewBalance();

  /**
   * The status of a balance transfer.
   *
   * @return
   */
  Status balanceTransferStatus();

  /**
   * Possible statuses of a balance transfer.
   */
  enum Status {
    NOT_ATTEMPTED,
    APPLIED,
    FAILED
  }

}
