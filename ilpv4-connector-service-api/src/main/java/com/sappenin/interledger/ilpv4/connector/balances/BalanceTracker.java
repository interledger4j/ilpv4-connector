package com.sappenin.interledger.ilpv4.connector.balances;

import org.interledger.connector.accounts.AccountId;

import java.math.BigInteger;
import java.util.UUID;

/**
 * <p>Tracks balances for accounts held at this connector.</p>
 *
 * <p>Unlike a traditional double-accounting-model `ledger` where all
 * balances move between two accounts, this mechanism is merely a single amount balance-tracker, so only one balance is
 * tracked. Each balance indicates the amount of units owed-by or owed-to this Connector by a particular account. In
 * this way, a double-entry accounting model can be implied by simply taking the inverse of this balance (to represent
 * the source/destination account of a movements of value in a "transfer" paradigm).</p>
 *
 * <p>In the balance-tracker paradigm, there are no "transfers" per-se. Instead, balances are incremented and
 * decremented, and the "transfer" to/from the counterparty is implied, though not formally tracked.</p>
 */
public interface BalanceTracker {

  AccountBalance getBalance(AccountId accountId);

  void resetBalance(AccountId accountId);

  /**
   * Increment the balance on an account in this Connector by {@code amount} units. Unlike a traditional ledger where
   * all balances move between two accounts, this mechanism is merely a balance-tracker, so only one balance is tracked
   * to indicate the amount of units owed-by or owed-to this Connector.
   *
   * For tracking purposes
   *
   * @param transactionId A unique identifier for this transaction, to support idempotence.
   * @param accountId     The Account to adjust.
   * @param amount        The amount of units to add to the balance (positive or negative).
   *
   * @return A {@link BalanceChangeResult} indicating the results of this transfer attempt.
   */
  BalanceChangeResult adjustBalance(
    UUID transactionId, AccountId accountId, BigInteger amount
  );
}
