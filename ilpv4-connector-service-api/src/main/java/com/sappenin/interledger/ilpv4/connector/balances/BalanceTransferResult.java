package com.sappenin.interledger.ilpv4.connector.balances;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;

import java.math.BigInteger;
import java.util.UUID;

@Value.Immutable
public interface BalanceTransferResult {

  static ImmutableBalanceTransferResult.Builder builder() {
    return ImmutableBalanceTransferResult.builder();
  }

  UUID balanceTransferId();

  /**
   * The {@link AccountId} for source of funds.
   *
   * @return
   */
  AccountId sourceAccountId();

  /**
   * The {@link AccountId} for source of funds.
   *
   * @return
   */
  AccountId destinationAccountId();

  /**
   * The amount of units currently in this account balance.
   *
   * @return
   */
  BigInteger transferAmount();

  BigInteger sourceAccountPreviousBalance();

  BigInteger sourceAccountBalance();

  BigInteger destinationAccountPreviousBalance();

  BigInteger destinationAccountBalance();

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
    PREPARED,
    EXECUTED,
    FAILED
  }

}
