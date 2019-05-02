package com.sappenin.interledger.ilpv4.connector.balances;

import com.google.common.collect.Maps;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks balances for accounts held at this connector by only storing and operating upon the account balance from the
 * perspective of this connector. For example, if an account exists with id `123`, then a positive balance in this
 * account indicates that the Connector owes the owner of account `123` whatever the balance is. Conversely, if that
 * same account has a negative balance, then the Connector is owed the balance in the account.
 */
public class InMemoryBalanceTracker implements BalanceTracker {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final AccountSettingsRepository accountSettingsRepository;
  //private final AccountManager accountManager;
  private final Map<AccountId, AtomicInteger> balances;

  public InMemoryBalanceTracker(final AccountSettingsRepository accountSettingsRepository) {
    this.balances = Maps.newConcurrentMap();
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
  }

  @Override
  public AccountBalance getBalance(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    final AccountSettings accountSettings = accountSettingsRepository.safeFindByAccountId(accountId);

    final AtomicInteger currentAmount = this.getBalanceNumber(accountId);

    // TODO: For performance, use a Modifiable variant in a cache.
    return AccountBalance.builder()
      .accountId(accountId)
      .amount(currentAmount)
      .assetCode(accountSettings.getAssetCode())
      .assetScale(accountSettings.getAssetScale())
      .build();
  }

  @Override
  public void resetBalance(AccountId accountId) {
    synchronized (this) {
      this.balances.put(accountId, new AtomicInteger());
    }
  }

  @Override
  public BalanceChangeResult adjustBalance(UUID transactionId, AccountId accountId, BigInteger amount) {

    try {
      if (amount.equals(BigInteger.ZERO)) {
        logger.warn("Encountered a BalanceChange Request with 0 amount, which is a no-op.");
        return BalanceChangeResult.builder()
          .balanceTransferId(transactionId)
          .accountId(accountId)
          .amount(amount)
          .balanceTransferStatus(BalanceChangeResult.Status.NOT_ATTEMPTED)
          .build();
      } else {

        final ImmutableBalanceChangeResult.Builder balanceChangeResultBuilder = BalanceChangeResult.builder()
          .balanceTransferId(transactionId)
          .accountId(accountId)
          .amount(amount)
          .balanceTransferStatus(BalanceChangeResult.Status.APPLIED);

        try {
          // If the balance isn't yet initialized, we need to initialize it in a thread-safe manner.
          final int accountPreviousBalance = this.getBalanceNumber(accountId).getAndAdd(amount.intValue());
          balanceChangeResultBuilder.accountPreviousBalance(BigInteger.valueOf(accountPreviousBalance));
          balanceChangeResultBuilder
            .accountNewBalance(BigInteger.valueOf(accountPreviousBalance + amount.negate().intValue()));
        } catch (RuntimeException e) {
          balanceChangeResultBuilder.accountNewBalance(Optional.empty());
          balanceChangeResultBuilder.accountPreviousBalance(Optional.empty());
          balanceChangeResultBuilder.balanceTransferStatus(BalanceChangeResult.Status.FAILED);
          throw new BalanceTrackerException(
            String.format(
              "Unable to apply BalanceChange: %s. Error: %s", balanceChangeResultBuilder.build(), e.getMessage()
            ), e);
        }

        // TODO: Log stats....
        //this.stats.balance.setValue(account, {}, balance.getValue().toNumber())

        final BalanceChangeResult balanceChangeResult = balanceChangeResultBuilder.build();
        // Use `trace` as opposed to `debug` so that we _could_ debug under very heavy load without seeing _many_ of these.
        logger.trace("BalanceChangeResult: {}", balanceChangeResult);
        return balanceChangeResult;
      }
    } catch (BalanceTrackerException e) {
      throw e;
    } catch (Exception e) {
      throw new BalanceTrackerException(e.getMessage(), e);
    }
  }


  //  @VisibleForTesting
  //  protected void increaseAccountBalance(final AccountId accountId, final BigInteger amount) {
  //    // Ignore 0-amount packets.
  //
  //      // Increase balance of accountId on prepare
  //      final BalanceChangeResult balanceChangeResult = this.balanceTracker.adjustBalance(
  //        UUID.randomUUID(), accountId, amount
  //      );
  //      logger.debug(
  //        "Account balance increased due to incoming ilp prepare. BalanceChangeResult: {}", balanceChangeResult
  //      );
  //      // TODO: Log stats....
  //      //this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
  //  }

  //  @VisibleForTesting
  //  protected void decreaseAccountBalance(final AccountId accountId, final BigInteger amount,
  //                                        final String errorMessagePrefix) {
  //    // Ignore 0-amount packets.
  //    if (amount.equals(BigInteger.ZERO)) {
  //      return;
  //    } else {
  //      // Decrease balance of accountId
  //      final BalanceChangeResult balanceChangeResult = this.balanceTracker.adjustBalance(
  //        UUID.randomUUID(), accountId, amount.negate()
  //      );
  //      logger.debug("{}; BalanceChangeResult: {}", errorMessagePrefix, balanceChangeResult);
  //      // TODO: Log stats....
  //      // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
  //      // this.stats.incomingDataPacketValue.increment(account, { result : 'failed' }, + amount)
  //    }
  //}

  //  @Override
  //  public BalanceChangeResult transferUnits(
  //    UUID transactionId, AccountId sourceAccountId, AccountId destinationAccountId, BigInteger amount
  //  ) {
  //
  //    // If the balance isn't yet initialized, we need to initialize it in a thread-safe mannger.
  //
  //
  //    final int sourceAccountPreviousBalance =
  //      this.getBalanceNumber(sourceAccountId).getAndAdd(amount.negate().intValue());
  //    final int currentSourceAccountBalance = sourceAccountPreviousBalance + amount.negate().intValue();
  //
  //    final int destinationAccountPreviousBalance =
  //      this.getBalanceNumber(destinationAccountId).getAndAdd(amount.intValue());
  //    final int currentDestinationAccountBalance = destinationAccountPreviousBalance + amount.intValue();
  //
  //    final BalanceChangeResult balanceChangeResult = BalanceChangeResult.builder()
  //      .balanceTransferId(transactionId)
  //      .sourceAccountId(sourceAccountId)
  //      .sourceAccountPreviousBalance(BigInteger.valueOf(sourceAccountPreviousBalance))
  //      .sourceAccountBalance(BigInteger.valueOf(currentSourceAccountBalance))
  //      .transferAmount(amount)
  //      .destinationAccountId(destinationAccountId)
  //      .destinationAccountPreviousBalance(BigInteger.valueOf(destinationAccountPreviousBalance))
  //      .destinationAccountBalance(BigInteger.valueOf(currentDestinationAccountBalance))
  //      .balanceTransferStatus(BalanceChangeResult.Status.EXECUTED)
  //      .build();
  //
  //    logger.trace("BalanceChangeResult: {}", balanceChangeResult);
  //    return balanceChangeResult;
  //  }

  /**
   * Get the balance, initializing it to 0 if required.
   *
   * @param accountId
   *
   * @return
   */
  private AtomicInteger getBalanceNumber(final AccountId accountId) {

    // Initial check so we don't synchronize every happy-path call...
    if (balances.get(accountId) == null) {
      // If we get here, it means the account was not initialized. Under load, we must block multiple threads
      // from creating the same balance and accidentally stepping on an existing balance, but this only needs to occur
      // for a "create account" operation.
      synchronized (this) {
        // Only allow 1 thread to initialize the balance...
        if (balances.get(accountId) == null) {
          // If no account, initialize a new one with a 0 balance.
          final AtomicInteger zeroBalance = new AtomicInteger();
          balances.put(accountId, zeroBalance);
        }
      }
    }

    // This will return the correct AtomicInteger for the accountId...
    return balances.get(accountId);
  }
}