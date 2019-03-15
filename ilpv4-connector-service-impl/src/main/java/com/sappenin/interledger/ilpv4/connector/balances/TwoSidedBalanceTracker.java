package com.sappenin.interledger.ilpv4.connector.balances;

/**
 * Tracks balances for accounts held at this connector.
 */
public interface TwoSidedBalanceTracker {

//  // TODO: Remove the tracking account. We always know the other side because it's the negation of the real account.
//  @Deprecated
//  String TRACKING_ACCOUNT_SUFFIX = "-TrackingAccount";
//
//  AccountBalance getBalance(AccountId accountId);
//
//  void resetBalance(AccountId accountId);
//
//  /**
//   * Transfer {@code amount} units from {@code sourceAccountId} to the account identified by {@code
//   * destinationAccountId}.
//   *
//   * @param transactionId   A unique identifier for this transaction, to support idempotence.
//   * @param sourceAccountId The Account to withdraw units from.
//   * @param targetAccountId The account to deposit units into.
//   * @param amount          The amount of units to transfer.
//   *
//   * @return A {@link BalanceChangeResult} indicating the results of this transfer attempt.
//   */
//  BalanceChangeResult transferUnits(
//    UUID transactionId, AccountId sourceAccountId, AccountId targetAccountId, BigInteger amount
//  );
//
//  /**
//   * An in-memory implementation of {@link TwoSidedBalanceTracker}.
//   *
//   * This implementation should not be used in production because it does not persist any account balances, and always
//   * initializes them to zero if no account exists. Instead, a production-worthy implementation should persist these
//   * account balances across server restarts (and to enable a stateless connector).
//   */
//  class InMemoryBalanceTracker implements TwoSidedBalanceTracker {
//
//    private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//    private final AccountManager accountManager;
//    private final Map<AccountId, AtomicInteger> balances;
//
//    public InMemoryBalanceTracker(final AccountManager accountManager) {
//      this.balances = Maps.newConcurrentMap();
//      this.accountManager = Objects.requireNonNull(accountManager);
//    }
//
//    @Override
//    public AccountBalance getBalance(AccountId accountId) {
//      final AccountSettings accountSettings = accountManager.safeGetAccount(accountId).getAccountSettings();
//
//      final AtomicInteger currentAmount = this.getBalanceNumber(accountId);
//
//      // TODO: For performance, use a Modifiable variant in a cache.
//      return AccountBalance.builder()
//        .accountId(accountId)
//        .amount(currentAmount)
//        .assetCode(accountSettings.getAssetCode())
//        .assetScale(accountSettings.getAssetScale())
//        .build();
//    }
//
//    @Override
//    public void resetBalance(AccountId accountId) {
//      synchronized (this) {
//        this.balances.put(accountId, new AtomicInteger());
//      }
//    }
//
//    @Override
//    public BalanceChangeResult transferUnits(
//      UUID transactionId, AccountId sourceAccountId, AccountId destinationAccountId, BigInteger amount
//    ) {
//
//      // If the balance isn't yet initialized, we need to initialize it in a thread-safe mannger.
//
//
//      final int sourceAccountPreviousBalance =
//        this.getBalanceNumber(sourceAccountId).getAndAdd(amount.negate().intValue());
//      final int currentSourceAccountBalance = sourceAccountPreviousBalance + amount.negate().intValue();
//
//      final int destinationAccountPreviousBalance =
//        this.getBalanceNumber(destinationAccountId).getAndAdd(amount.intValue());
//      final int currentDestinationAccountBalance = destinationAccountPreviousBalance + amount.intValue();
//
//      final BalanceChangeResult balanceChangeResult = BalanceChangeResult.builder()
//        .balanceTransferId(transactionId)
//        .sourceAccountId(sourceAccountId)
//        .sourceAccountPreviousBalance(BigInteger.valueOf(sourceAccountPreviousBalance))
//        .sourceAccountBalance(BigInteger.valueOf(currentSourceAccountBalance))
//        .transferAmount(amount)
//        .destinationAccountId(destinationAccountId)
//        .destinationAccountPreviousBalance(BigInteger.valueOf(destinationAccountPreviousBalance))
//        .destinationAccountBalance(BigInteger.valueOf(currentDestinationAccountBalance))
//        .balanceTransferStatus(BalanceChangeResult.Status.EXECUTED)
//        .build();
//
//      logger.trace("BalanceChangeResult: {}", balanceChangeResult);
//      return balanceChangeResult;
//    }
//
//    /**
//     * Get the balance, initializing it to 0 if required.
//     *
//     * @param accountId
//     *
//     * @return
//     */
//    private AtomicInteger getBalanceNumber(final AccountId accountId) {
//
//      // Initial check so we don't synchronize every happy-path call...
//      if (balances.get(accountId) == null) {
//        // If we get here, it means the account was not initialized. Under load, we must block multiple threads
//        // from creating the same balance and accidentally stepping on an existin balance.
//        synchronized (this) {
//          // Only allow 1 thread to set the balance...
//          if (balances.get(accountId) == null) {
//            // If no account, initialize a new one with a 0 balance.
//            final AtomicInteger zeroBalance = new AtomicInteger();
//            balances.put(accountId, zeroBalance);
//          }
//        }
//      }
//
//      // This will return the correct AtomicInteger...
//      return balances.get(accountId);
//    }
//  }
}
