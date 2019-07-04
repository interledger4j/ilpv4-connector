package com.sappenin.interledger.ilpv4.connector.balances;

import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.core.settlement.Quantity;

import java.util.Optional;
import java.util.UUID;

/**
 * <p>Tracks balances for accounts held at this connector.</p>
 *
 * <p>For each account, this Connector tracks a clearing balance (as a signed 64-bit integer) that represents the net
 * position with the account owner. A positive clearing balance indicates the Connector operator has an outstanding
 * liability (owes money) to the account holder. A negative clearing balance represents an asset (the account holder
 * owes money to the operator).</p>
 *
 * <p>In the balance-tracker paradigm, a double-entry accounting model can be implied from any given balance(see
 * above), but only on a net basis. In order to track a full accounting of all balance changes, packets must be
 * separately logged and accumulated using some external reporting system such as Google BigQuery.</p>
 *
 * <p>As an implementation note, the design for this interface is taken from the discussions had in the Interledger
 * Forum
 * <a href="https://forum.interledger.org/t/what-should-positive-negative-balances-represent/501/26?u=sappenin">here</a>.
 * In other words, the implementation track a single clearing balance number and, for nodes that accept pre-payment,
 * have a separate number to track prepaid funds. All ILP packets affect the normal net clearing balance position, and
 * outgoing settlements can be based upon this position. When an implementation receives an incoming settlement, it will
 * clear out as much of the peer’s debt as the settlement covers and any extra would be put into the prepaid funds
 * account.</p>
 */
public interface BalanceTracker {

  /**
   * Retrieve the current balance for the supplied {@code accountId}.
   *
   * @param accountId An {@link AccountId} representing an account whose balance is stored in this tracker.
   *
   * @return An instance of {@link AccountBalance}.
   */
  AccountBalance getBalance(AccountId accountId);

  /**
   * Called in response to an ILP Prepare packet, atomically updates the balances for a source and destination account
   * according to the passed-in information. This method does not take into consideration any sort of minimum balance.
   *
   * @param sourceAccountId The {@link AccountId} of the account balance to adjust.
   * @param sourceAmount    A {@link long} representing a positive amount of units to subtract from =the source
   *                        account's balance.
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  default void updateBalanceForPrepare(AccountId sourceAccountId, long sourceAmount) throws BalanceTrackerException {
    updateBalanceForPrepare(sourceAccountId, sourceAmount, Optional.empty());
  }

  /**
   * Called in response to an ILP Prepare packet, atomically updates the balances for a source and destination account
   * according to the passed-in information.
   *
   * @param sourceAccountId The {@link AccountId} of the account balance to adjust.
   * @param sourceAmount    The positive amount of units (as an unsigned long) to subtract from the source account's
   *                        balance.
   * @param minBalance      An optionally-present {@link Long} representing the minimum balance that the source account
   *                        is allowed to reduce to (typed as {@link Long} in order to allow a large negative balance up
   *                        to the width of a long number).
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForPrepare(
    AccountId sourceAccountId, long sourceAmount, Optional<Long> minBalance
  ) throws BalanceTrackerException;

  /**
   * Called in response to an ILP Fulfill packet. Atomically updates the balances for a destination account according to
   * the passed-in information (i.e., because we received a Fulfill response, we should credit the destination
   * counterparty account).
   *
   * @param destinationAccountId The {@link AccountId} of the destination balance to adjust.
   * @param destinationAmount    The positive amount of units (as an unsigned long) to add to the destination account's
   *                             balance.
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForFulfill(AccountId destinationAccountId, long destinationAmount) throws BalanceTrackerException;

  /**
   * Called in response to an ILP Reject packet, atomically updates the balances for a source and destination account
   * according to the passed-in information (i.e., because we received a Reject response, we should credit the source
   * counterparty account that we debited during the prepare phase).
   *
   * @param sourceAccountId The {@link AccountId} of the account balance to adjust.
   * @param sourceAmount    The positive amount of units (as an unsigned long) to add to the source account's balance
   *                        (always positive).
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForReject(AccountId sourceAccountId, long sourceAmount) throws BalanceTrackerException;

  /**
   * Called in response to an ILP Reject packet, atomically updates the balances for a source and destination account
   * according to the passed-in information (i.e., because we received a Reject response, we should credit the source
   * counterparty account that we debited during the prepare phase).
   *
   * @param idempotencyKey  A {@link UUID} that acts as a key for idempotence.
   * @param sourceAccountId The {@link AccountId} of the account balance to adjust.
   * @param scaledQuantity  A {@link Quantity} of units that has been scaled from scaled of the underlying Settlement
   *                        Ledger into the units of the ILP clearing ledger, which has the same scale as the account
   *                        owned by {@code sourceAccountId}.
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForSettlement(UUID idempotencyKey, AccountId sourceAccountId, Quantity scaledQuantity) throws BalanceTrackerException;
}
