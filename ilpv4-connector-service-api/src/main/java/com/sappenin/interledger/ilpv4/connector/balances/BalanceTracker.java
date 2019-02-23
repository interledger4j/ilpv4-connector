package com.sappenin.interledger.ilpv4.connector.balances;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks balances for accounts held at this connector.
 */
public interface BalanceTracker {

  String TRACKING_ACCOUNT_SUFFIX = "-TrackingAccount";

  AccountBalance getBalance(AccountId accountId);

  // TODO: Consider putting all funds "on-hold" during the prepare step, and then either executing that transfer or
  //  rolling it back during the fufill/reject. This might be overkill in a Connector processing very small payments,
  //  this is not implemented currently because, in general, it should be easy to just transfer units preemptively,
  //  and then claw them back on a reject.
  //void holdFunds(UUID transactionId, AccountId sourceAccountId, AccountId destinationAccount, BigInteger amount);

  /**
   * Transfer {@code amount} units from {@code sourceAccountId} to the account identified by {@code
   * destinationAccountId}.
   *
   * @param transactionId   A unique identifier for this transaction, to support idempotence.
   * @param sourceAccountId The Account to withdraw units from.
   * @param targetAccountId The account to deposit units into.
   * @param amount          The amount of units to transfer.
   *
   * @return A {@link BalanceTransferResult} indicating the results of this transfer attempt.
   */
  BalanceTransferResult transferUnits(
    UUID transactionId, AccountId sourceAccountId, AccountId targetAccountId, BigInteger amount
  );

  /**
   * An in-memory implementation of {@link BalanceTracker}.
   *
   * This implementation should not be used in production because it does not persist any account balances, and always
   * initializes them to zero if no account exists. Instead, a production-worthy implementation should persist these
   * account balances across server restarts (and to enable a stateless connector).
   */
  class InMemoryBalanceTracker implements BalanceTracker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AccountManager accountManager;
    private final Map<AccountId, AccountBalance> balances;

    public InMemoryBalanceTracker(final AccountManager accountManager) {
      this.balances = Maps.newConcurrentMap();
      this.accountManager = Objects.requireNonNull(accountManager);
    }

    @Override
    public AccountBalance getBalance(AccountId accountId) {
      return Optional.ofNullable(balances.get(accountId))
        .orElseGet(() -> {
          final AccountSettings accountSettings = accountManager.safeGetAccount(accountId).getAccountSettings();
          // If no account, initialize a new one with a 0 balance.
          balances.put(accountId,
            AccountBalance.builder()
              .accountId(accountId)
              .amount(new AtomicInteger())
              .assetCode(accountSettings.getAssetCode())
              .assetScale(accountSettings.getAssetScale())
              .build());
          return balances.get(accountId);
        });
    }

    @Override
    public BalanceTransferResult transferUnits(
      UUID transactionId, AccountId sourceAccountId, AccountId destinationAccountId, BigInteger amount
    ) {
      int sourceAccountPreviousBalance = this.getBalance(sourceAccountId)
        .getAmount()
        .getAndAdd(amount.negate().intValue());

      int currentSourceAccountBalance = sourceAccountPreviousBalance + amount.negate().intValue();

      int destinationAccountPreviousBalance = this.getBalance(destinationAccountId)
        .getAmount()
        .getAndAdd(amount.intValue());
      int currentDestinationAccountBalance = destinationAccountPreviousBalance + amount.intValue();

      final BalanceTransferResult balanceTransferResult = BalanceTransferResult.builder()
        .balanceTransferId(transactionId)
        .sourceAccountId(sourceAccountId)
        .sourceAccountPreviousBalance(BigInteger.valueOf(sourceAccountPreviousBalance))
        .sourceAccountBalance(BigInteger.valueOf(currentSourceAccountBalance))
        .transferAmount(amount)
        .destinationAccountId(destinationAccountId)
        .destinationAccountPreviousBalance(BigInteger.valueOf(destinationAccountPreviousBalance))
        .destinationAccountBalance(BigInteger.valueOf(currentDestinationAccountBalance))
        .balanceTransferStatus(BalanceTransferResult.Status.EXECUTED)
        .build();

      logger.info("BalanceTransferResult: {}", balanceTransferResult);
      return balanceTransferResult;
    }

  }
}
