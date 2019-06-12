package org.interledger.ilpv4.connector.balances;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountId;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks balances in-memory only.
 *
 * Note that this implementation is not meant for production usage, so it is not thread-safe. Instead, this
 * implementation exists to facilitate Spring-container tests involving only a single server.
 *
 * For multi-server tests, or actual use-cases, consider using {@link RedisBalanceTracker}.
 */
public class InMemoryBalanceTracker implements BalanceTracker {

  //private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Map<AccountId, BigInteger> balances;
  private final Map<AccountId, BigInteger> prepaidBalances;

  public InMemoryBalanceTracker() {
    this.balances = Maps.newConcurrentMap();
    this.prepaidBalances = Maps.newConcurrentMap();
  }

  @Override
  public AccountBalance getBalance(AccountId accountId) {
    return AccountBalance.builder()
      .accountId(accountId)
      .balance(Optional.ofNullable(this.balances.get(accountId)).orElse(BigInteger.ZERO))
      .prepaidAmount(Optional.ofNullable(this.prepaidBalances.get(accountId)).orElse(BigInteger.ZERO))
      .build();
  }

  @Override
  public BigInteger updateBalanceForPrepare(AccountId sourceAccountId, BigInteger sourceAmount, Optional<BigInteger> minBalance) throws BalanceTrackerException {
    final AccountBalance accountBalance = this.getBalance(sourceAccountId);

    // Throw an exception if minBalance is violated....
    minBalance.ifPresent(mb -> {
      if (accountBalance.netBalance().subtract(sourceAmount).compareTo(mb) < 0) {
        throw new BalanceTrackerException(String.format(
          "Incoming prepare of %s would bring account %s under its minimum balance. Current balance: %s, min balance: %s",
          sourceAmount, sourceAccountId, accountBalance.netBalance(), mb)
        );
      }
    });

    if (accountBalance.prepaidAmount().compareTo(sourceAmount) >= 0) {
      // Reduce prepaid_amount by 1
      this.prepaidBalances.put(sourceAccountId, accountBalance.prepaidAmount().subtract(BigInteger.ONE));
    } else if (accountBalance.prepaidAmount().compareTo(BigInteger.ZERO) >= 0) {
      final BigInteger subFromBalance = sourceAmount.subtract(accountBalance.prepaidAmount());
      this.prepaidBalances.put(sourceAccountId, BigInteger.ZERO);
      this.balances.put(sourceAccountId, accountBalance.balance().subtract(subFromBalance));
    } else {
      // Decrement the balance by 1
      this.balances.put(sourceAccountId, accountBalance.balance().subtract(BigInteger.ONE));
    }

    return getBalance(sourceAccountId).netBalance();
  }

  @Override
  public BigInteger updateBalanceForFulfill(AccountId destinationAccountId, BigInteger destinationAmount) throws BalanceTrackerException {
    Optional.ofNullable(this.balances.get(destinationAccountId)).orElse(BigInteger.ZERO).add(destinationAmount);
    return getBalance(destinationAccountId).netBalance();
  }

  @Override
  public BigInteger updateBalanceForReject(AccountId sourceAccountId, BigInteger sourceAmount) throws BalanceTrackerException {
    Optional.ofNullable(this.balances.get(sourceAccountId)).orElse(BigInteger.ZERO).add(sourceAmount);
    return getBalance(sourceAccountId).netBalance();
  }

  public void resetBalance(AccountId accountId) {
    this.balances.put(accountId, BigInteger.ZERO);
    this.prepaidBalances.put(accountId, BigInteger.ZERO);
  }
}