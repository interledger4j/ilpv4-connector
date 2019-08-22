package org.interledger.connector.balances;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.core.settlement.SettlementQuantity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks balances in-memory in a thread-safe manner.
 *
 * Note that this implementation is not meant for production usage because the values it tracks are not durable.
 * Instead, consider using {@link RedisBalanceTracker} or another persistent implementation of {@link BalanceTracker}.
 */
public class InMemoryBalanceTracker implements BalanceTracker {

  private final Map<AccountId, AtomicLong> clearingBalances;
  private final Map<AccountId, AtomicLong> prepaidBalances;

  public InMemoryBalanceTracker() {
    this.clearingBalances = Maps.newConcurrentMap();
    this.prepaidBalances = Maps.newConcurrentMap();
  }

  @Override
  public AccountBalance getBalance(AccountId accountId) {
    return AccountBalance.builder()
      .accountId(accountId)
      .clearingBalance(getOrCreateBalance(this.clearingBalances, accountId).longValue())
      .prepaidAmount(getOrCreateBalance(this.prepaidBalances, accountId).longValue())
      .build();
  }

  @Override
  public void updateBalanceForPrepare(
    AccountId sourceAccountId, long amount, Optional<Long> minBalance
  ) throws BalanceTrackerException {
    final AccountBalance accountBalance = this.getBalance(sourceAccountId);

    // Throw an exception if minBalance is violated....
    minBalance.ifPresent(mb -> {
      if (accountBalance.netBalance().longValue() - amount < mb) {
        throw new BalanceTrackerException(String.format(
          "Incoming prepare of %s would bring account %s under its minimum balance. Current balance: %s, min balance: %s",
          amount, sourceAccountId, accountBalance.netBalance(), mb)
        );
      }
    });

    if (accountBalance.prepaidAmount() >= amount) {
      // Reduce prepaid_amount by 1
      this.decrement(prepaidBalances, sourceAccountId, amount);
    } else if (accountBalance.prepaidAmount() >= 0L) {
      final long subFromBalance = amount - accountBalance.prepaidAmount();
      this.prepaidBalances.put(sourceAccountId, new AtomicLong());
      this.decrement(clearingBalances, sourceAccountId, subFromBalance);
    } else {
      // Decrement the clearingBalance by `sourceAmount`
      this.decrement(this.clearingBalances, sourceAccountId, amount);
    }
  }

  @Override
  public UpdateBalanceForFulfillResponse updateBalanceForFulfill(
    final AccountSettings destinationAccountSettings, final long amount
  ) throws BalanceTrackerException {
    this.increment(this.clearingBalances, destinationAccountSettings.getAccountId(), amount);

    final AccountBalance currentBalance = this.getBalance(destinationAccountSettings.getAccountId());
    final long amountToSettle = this.computeSettlementQuantity(destinationAccountSettings, currentBalance);

    return UpdateBalanceForFulfillResponse.builder()
      .accountBalance(currentBalance)
      .clearingAmountToSettle(amountToSettle)
      .build();
  }

  @Override
  public void updateBalanceForReject(AccountId sourceAccountId, long amount) throws BalanceTrackerException {
    this.increment(this.clearingBalances, sourceAccountId, amount);
  }

  @Override
  public void updateBalanceForIncomingSettlement(String idempotencyKey, AccountId accountId, long amount) throws BalanceTrackerException {
    this.increment(this.clearingBalances, accountId, amount);
  }

  @Override
  public void updateBalanceForOutgoingSettlementRefund(AccountId accountId, long amount) throws BalanceTrackerException {
    this.increment(this.clearingBalances, accountId, amount);
  }

  /**
   * Exists only for testing, but is purposefully not in the parent interface.
   */
  @VisibleForTesting
  public void resetAllBalances() {
    this.clearingBalances.clear();
    this.prepaidBalances.clear();
  }

  private void increment(
    final Map<AccountId, AtomicLong> balanceTracker, final AccountId accountId, final long amount
  ) {
    final AtomicLong balance = getOrCreateBalance(balanceTracker, accountId);
    balance.getAndAdd(amount);
  }

  private void decrement(
    final Map<AccountId, AtomicLong> balanceTracker, final AccountId accountId, final long amount
  ) {
    final AtomicLong balance = getOrCreateBalance(balanceTracker, accountId);
    balance.getAndAdd(0 - amount);
  }

  private synchronized AtomicLong getOrCreateBalance(
    final Map<AccountId, AtomicLong> balanceTracker, final AccountId accountId
  ) {
    Objects.requireNonNull(balanceTracker);
    Objects.requireNonNull(accountId);

    return Optional.ofNullable(balanceTracker.get(accountId))
      .orElseGet(() -> {
        balanceTracker.put(accountId, new AtomicLong());
        return balanceTracker.get(accountId);
      });
  }

  /**
   * <p>Compute a {@link SettlementQuantity} based upon the current balance of an account. Using the {@link
   * AccountBalanceSettings#getSettleThreshold()} and {@link AccountBalanceSettings#getSettleTo()}, this method can
   * compute the amount of a settlement payment by determining if the current balance exceeds the settlement threshold,
   * and if so, by how much.</p>
   *
   * <p>Note that this method returns a {@link SettlementQuantity} with the scale of the ILP clearing layer, and NOT
   * with the scale of the settlement layer, if these two values diverge.</p>
   *
   * @param accountSettings A {@link AccountSettings} for the account to compute a settlement payment amount for.
   * @param accountBalance  A {@link AccountBalance} containing a snapshot in time of the account's balance (note this
   *                        value may be stale).
   *
   * @return An optionally-present {@link SettlementQuantity} with the scale of the ILP clearing layer.
   */
  private long computeSettlementQuantity(
    final AccountSettings accountSettings, final AccountBalance accountBalance
  ) {
    Objects.requireNonNull(accountSettings, "accountSettings must not be null");
    Objects.requireNonNull(accountBalance, "accountBalance must not be null");

    final long settleTo = accountSettings.getBalanceSettings().getSettleTo();
    final long clearingBalance = accountBalance.clearingBalance();

    return accountSettings.getBalanceSettings().getSettleThreshold()
      // If there is a settle_threshold, we need to return the propert settlement quantity, if any.
      .map(settleThreshold -> {
        final long settlementAmount;
        if (clearingBalance > settleThreshold && clearingBalance > settleTo) {
          settlementAmount = clearingBalance - settleTo;
        } else {
          settlementAmount = 0;
        }
        return settlementAmount;
      })
      .orElse(0L);
  }
}
