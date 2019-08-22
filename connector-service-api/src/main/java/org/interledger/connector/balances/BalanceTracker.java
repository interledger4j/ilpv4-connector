package org.interledger.connector.balances;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;

import java.util.Optional;

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
 * Forum (link below). In other words, the implementation track a single clearing balance number and, for nodes that
 * accept pre-payment, have a separate number to track prepaid funds. All ILP packets affect the normal net clearing
 * balance position, and outgoing settlements can be based upon this position. When an implementation receives an
 * incoming settlement, it will clear out as much of the peer’s debt as the settlement covers and any extra would be put
 * into the prepaid funds account.</p>
 *
 * <p>Finally, it's important to note that balance tracking is not required in Interleger. A primary reason to
 * track a balance with a peer is if you want to limit the `min_balance` (i.e., an upper bound on the amount of credit
 * to extend a peer). If you don't want to limit this amount, atomically updating a single number in a balance tracker
 * will be a bottleneck for what could be a higher-throughput system. See discussion below in interledger-rs#159.</p>
 *
 * @see "https://github.com/emschwartz/interledger-rs/issues/159"
 * @see "https://forum.interledger.org/t/what-should-positive-negative-balances-represent/501/26"
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
   * Called in response to an ILP Prepare packet, this function atomically updates the balance for the account
   * corresponding to the link that the packet was received on (i.e., the incoming link). This balance update will
   * subtract {@code amount} from the <tt>prepaid</tt> and <tt>clearing</tt> balances in the balance tracker.
   *
   * @param sourceAccountId The {@link AccountId} to adjust. This id corresponds to the link that a prepare packet was
   *                        received on.
   * @param amount          The positive amount of units (as an unsigned long) to subtract from the account's balance.
   * @param minBalance      An optionally-present {@link Long} representing the minimum balance that the account is
   *                        allowed to reduce to (typed as {@link Long} in order to allow a large negative balance up to
   *                        the width of a long number).
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForPrepare(AccountId sourceAccountId, long amount, Optional<Long> minBalance) throws BalanceTrackerException;

  /**
   * Called in response to an ILP Fulfill packet, this function atomically updates the balance for the account
   * corresponding to the link that the fulfill packet was received on (i.e., the outgoing link, and because we received
   * a Fulfill response, we should credit the destination counterparty account). This balance update will add {@code
   * amount} to the balances in the balance tracker according to specific rules (see implementations for details).
   *
   * @param destinationAccountSettings The {@link AccountSettings} of the account balance to adjust.
   * @param amount                     The positive amount of units (as an unsigned long) to add to the destination
   *                                   account's balance.
   *
   * @return An {@link AccountBalance} that contains the current state of the balance. Note that this value _may_ be
   * stale, depending on the load in the Connector.
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  UpdateBalanceForFulfillResponse updateBalanceForFulfill(AccountSettings destinationAccountSettings, long amount) throws BalanceTrackerException;

  /**
   * Called in response to an ILP Reject packet, this function atomically updates the balance for the account *
   * corresponding to the link that corresponding prepare packet was received on (i.e., the incoming link, and because
   * the transaction rejected, we should credit the source counterparty account which had units debited during the
   * prepare phase).
   *
   * @param sourceAccountId The {@link AccountId} of the account balance to adjust.
   * @param amount          The positive amount of units (as an unsigned long) to add to the source account's balance
   *                        (always positive).
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForReject(AccountId sourceAccountId, long amount) throws BalanceTrackerException;

  /**
   * <p>Increase the balance for the account identified by {@code accountId} by {@code amount} in response to an
   * incoming settlement payment</p>.
   *
   * <p>When a counterparty wants to settle with this Connector, it will send a payment on an underlying ledger.
   * A settlement engine will detect this payment and callback to this connector to inform the Connector that a
   * settlement payment has been received. The Connector will then update its balances to account for this incoming
   * payment.</p>
   *
   * @param accountId The {@link AccountId} of the account balance to adjust.
   * @param amount    An unsigned {@link long} that reflects the number of balance units to adjust. Note that this value
   *                  should be in the proper scale for the ILP account as found in {@link
   *                  AccountSettings#getAssetScale()}.
   *
   * @throws BalanceTrackerException If anything prevents the balance updates to succeed atomically.
   */
  void updateBalanceForIncomingSettlement(String idempotencyKey, AccountId accountId, long amount);

  // TODO: Add javadoc!
  void updateBalanceForOutgoingSettlementRefund(AccountId accountId, long amount) throws BalanceTrackerException;

  /**
   * A wrapper object that holds the response from Redis after the updateBalanceForFulfill script has executed.
   */
  @Value.Immutable
  interface UpdateBalanceForFulfillResponse {

    static ImmutableUpdateBalanceForFulfillResponse.Builder builder() {
      return ImmutableUpdateBalanceForFulfillResponse.builder();
    }

    /**
     * The current clearing balance in the Redis balance tracker.
     */
    AccountBalance accountBalance();

    /**
     * The amount calculated for settlement by the Redis script, in the clearing_layer account units.
     */
    long clearingAmountToSettle();
  }

}
