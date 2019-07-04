package org.interledger.ilpv4.connector.balances;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.core.settlement.Quantity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
    AccountId sourceAccountId, long sourceAmount, Optional<Long> minBalance
  ) throws BalanceTrackerException {
    final AccountBalance accountBalance = this.getBalance(sourceAccountId);

    // Throw an exception if minBalance is violated....
    minBalance.ifPresent(mb -> {
      if (accountBalance.netBalance().longValue() - sourceAmount < mb) {
        throw new BalanceTrackerException(String.format(
          "Incoming prepare of %s would bring account %s under its minimum balance. Current balance: %s, min balance: %s",
          sourceAmount, sourceAccountId, accountBalance.netBalance(), mb)
        );
      }
    });

    if (accountBalance.prepaidAmount() >= sourceAmount) {
      // Reduce prepaid_amount by 1
      this.decrement(prepaidBalances, sourceAccountId, sourceAmount);
    } else if (accountBalance.prepaidAmount() >= 0L) {
      final long subFromBalance = sourceAmount - accountBalance.prepaidAmount();
      this.prepaidBalances.put(sourceAccountId, new AtomicLong());
      this.decrement(clearingBalances, sourceAccountId, subFromBalance);
    } else {
      // Decrement the clearingBalance by `sourceAmount`
      this.decrement(this.clearingBalances, sourceAccountId, sourceAmount);
    }
  }

  @Override
  public void updateBalanceForFulfill(AccountId destinationAccountId, long destinationAmount) throws BalanceTrackerException {
    this.increment(this.clearingBalances, destinationAccountId, destinationAmount);
  }

  @Override
  public void updateBalanceForReject(AccountId sourceAccountId, long sourceAmount) throws BalanceTrackerException {
    this.increment(this.clearingBalances, sourceAccountId, sourceAmount);
  }

  @Override
  public void updateBalanceForSettlement(UUID idempotencyKey, AccountId sourceAccountId, Quantity scaledQuantity) throws BalanceTrackerException {

    final AccountBalance accountBalance = this.getBalance(sourceAccountId);

    if (accountBalance.clearingBalance() >= 0L) {
      // Increment prepaid_amount by the `amount
      this.increment(prepaidBalances, sourceAccountId, scaledQuantity.amount().longValue());
    } else if (Math.abs(accountBalance.clearingBalance()) >= scaledQuantity.amount().longValue()) {
      // Increment balance by the `amount`
      this.increment(clearingBalances, sourceAccountId, scaledQuantity.amount().longValue());
    } else {

      this.increment(prepaidBalances, sourceAccountId,
        scaledQuantity.amount().add(accountBalance.netBalance()).longValue());
      this.clearingBalances.put(sourceAccountId, new AtomicLong());
    }
  }

  public void resetBalance(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    this.clearingBalances.put(accountId, new AtomicLong());
    this.prepaidBalances.put(accountId, new AtomicLong());
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
}
