package org.interledger.connector.balances;

import com.google.common.base.Preconditions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singletonList;

/**
 * An implementation of {@link BalanceTracker} that uses Redis to track all balances. Note that Redis does not support
 * INCR/DECR operations on unsigned longs, but is instead limited to sign-longs. This is an implementation detail of
 * this particular balance tracker, which will throw an exception if any negative amounts are supplied.
 */
public class RedisBalanceTracker implements BalanceTracker {

  public static final String CLEARING_BALANCE = "clearing_balance";
  public static final String PREPAID_AMOUNT = "prepaid_amount";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // The following are Lua scripts that are used to atomically execute the given logic inside Redis. This allows for
  // more complex logic without needing multiple round trips for messages to be sent to and from Redis, as well as
  // locks to ensure no other process is accessing Redis at the same time. Note that all scripts are injected so that
  // the SHA checksum is not recalculated on every execution.
  // For more information on scripting in Redis, see https://redis.io/commands/eval
  private final RedisScript<Long> updateBalanceForPrepareScript;
  private final RedisScript<List> updateBalanceForFulfillScript;
  private final RedisScript<Long> updateBalanceForRejectScript;
  private final RedisScript<Long> updateBalanceForIncomingSettlementScript;
  private final RedisScript<Long> updateBalanceForSettlementRefundScript;

  private final RedisTemplate<String, String> stringRedisTemplate;
  private final RedisTemplate<String, ?> jacksonRedisTemplate;

  public RedisBalanceTracker(
    final RedisScript<Long> updateBalanceForPrepareScript,
    final RedisScript<List> updateBalanceForFulfillScript,
    final RedisScript<Long> updateBalanceForRejectScript,
    final RedisScript<Long> updateBalanceForIncomingSettlementScript,
    final RedisScript<Long> updateBalanceForSettlementRefundScript,
    final RedisTemplate<String, String> stringRedisTemplate,
    final RedisTemplate<String, ?> jacksonRedisTemplate
  ) {
    this.updateBalanceForPrepareScript = Objects.requireNonNull(updateBalanceForPrepareScript);
    this.updateBalanceForFulfillScript = Objects.requireNonNull(updateBalanceForFulfillScript);
    this.updateBalanceForRejectScript = Objects.requireNonNull(updateBalanceForRejectScript);
    this.updateBalanceForIncomingSettlementScript = Objects.requireNonNull(updateBalanceForIncomingSettlementScript);
    this.updateBalanceForSettlementRefundScript = Objects.requireNonNull(updateBalanceForSettlementRefundScript);

    this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate);
    this.jacksonRedisTemplate = Objects.requireNonNull(jacksonRedisTemplate);
  }

  @Override
  public AccountBalance balance(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    final BoundHashOperations<String, String, String> result =
      stringRedisTemplate.boundHashOps(toRedisAccountsKey(accountId));

    return AccountBalance.builder()
      .accountId(accountId)
      .clearingBalance(toLong(result.get(CLEARING_BALANCE)))
      .prepaidAmount(toLong(result.get(PREPAID_AMOUNT)))
      .build();
  }

  @Override
  public void updateBalanceForPrepare(
    final AccountId sourceAccountId, final long amount, final Optional<Long> minBalance
  ) throws BalanceTrackerException {

    Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null");
    Objects.requireNonNull(minBalance, "minBalance must not be null");

    // This implementation doesn't support unsigned longs because Redis doesn't natively support unsigned longs, and
    // this implementation allows Redis to perform all arithmetic natively via Lua scripts. While we could
    // theoretically try to make this work, we dont expect _very large_ packet amounts in ILP, and if we encounter
    // them, it probably means the scaling is mis-configured. Additionally, balance tracking has particularly
    // sensitive performance requirements, so any computational savings we can get in ILPv4 the happy-path is preferred.
    Preconditions.checkArgument(amount >= 0, String.format("amount `%s` cannot be negative!", amount));
    minBalance.ifPresent($ -> Preconditions.checkArgument(
      amount >= 0, String.format("minBalance `%s` must be a positive signed long!", $)
    ));

    try {
      long result;
      if (minBalance.isPresent()) {
        result = stringRedisTemplate.execute(
          updateBalanceForPrepareScript,
          singletonList(toRedisAccountsKey(sourceAccountId)),
          // Arg1: from_amount
          amount + "",
          // Arg2: min_balance (optional)
          minBalance.map($ -> $ + "").orElse("0")
        );
      } else {
        result = stringRedisTemplate.execute(
          updateBalanceForPrepareScript,
          singletonList(toRedisAccountsKey(sourceAccountId)),
          // Arg1: from_amount
          amount + ""
        );
      }

      logger.debug(
        "Balance increased due to IlpPrepare. amount={} sourceAccountId={} result={}",
        amount, sourceAccountId, result
      );
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error handling prepare with sourceAmount `%s` from accountId `%s`", amount, sourceAccountId
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  @Override
  public UpdateBalanceForFulfillResponse updateBalanceForFulfill(
    final AccountSettings destinationAccountSettings, final long amount
  ) throws BalanceTrackerException {

    Objects.requireNonNull(destinationAccountSettings, "destinationAccountSettings must not be null");
    // See note in updateBalanceForPrepare for why this is not using unsigned longs
    Preconditions.checkArgument(
      amount >= 0,
      String.format("amount `%s` cannot be negative!", amount)
    );

    try {
      // Response Format: `{ clearing_balance, prepaid_amount, settle_amount }`
      final List<Long> response = jacksonRedisTemplate.execute(
        updateBalanceForFulfillScript,
        // Key1: accountId.
        singletonList(toRedisAccountsKey(destinationAccountSettings.accountId())),
        // Arg1: amount
        amount + "",
        // Arg2: settleThreshold
        destinationAccountSettings.balanceSettings().settleThreshold().map($ -> $ + "").orElse(""),
        // Arg3: settleTo
        destinationAccountSettings.balanceSettings().settleTo() + ""
      );

      Preconditions.checkArgument(
        response.size() == 3,
        String.format("Lua script returned invalid array values: %s", response)
      );

      // { clearing_balance, prepaid_amount, settle_amount }
      // Idx0: clearing_balance
      // Idx1: prepaid_balance
      // Idx2: settlement_amount
      final UpdateBalanceForFulfillResponse typedResponse =
        UpdateBalanceForFulfillResponse.builder()
          .accountBalance(AccountBalance.builder()
            .accountId(destinationAccountSettings.accountId())
            .clearingBalance(response.get(0))
            .prepaidAmount(response.get(1))
            .build()
          )
          .clearingAmountToSettle(response.get(2))
          .build();

      logger.trace(
        "Processed balance update for Fulfillment (requested_amount=`{}`) on outgoing account (`{}`). " +
          "Script Response: {}", amount, destinationAccountSettings, typedResponse
      );

      return typedResponse;
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error in updateBalanceForFulfill with amount `%s` for accountId `%s`", amount, destinationAccountSettings
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updateBalanceForReject(AccountId sourceAccountId, long amount) throws BalanceTrackerException {
    Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null");
    // See note in updateBalanceForPrepare for why this is not using unsigned longs
    Preconditions.checkArgument(
      amount >= 0,
      String.format("amount `%s` must be a positive signed long!", amount)
    );

    try {
      long clearingBalance = stringRedisTemplate.execute(
        updateBalanceForRejectScript,
        singletonList(toRedisAccountsKey(sourceAccountId)),
        // Arg1: from_amount
        amount + ""
      );

      logger.debug(
        "Processed reject for incoming amount: `{}`. Account `{}` has clearingBalance (including prepaid amount): `{}`",
        amount, sourceAccountId, clearingBalance
      );
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error handling reject packet with sourceAmount `%s` from accountId `%s`", amount, sourceAccountId
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updateBalanceForIncomingSettlement(
    final String idempotencyKey, final AccountId accountId, final long amount
  ) throws BalanceTrackerException {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");

    // See note in updateBalanceForPrepare for why this is not using unsigned longs
    Preconditions.checkArgument(
      amount >= 0,
      String.format("amount `%s` must be a positive signed long!", amount)
    );

    try {
      long result = stringRedisTemplate.execute(
        updateBalanceForIncomingSettlementScript,
        singletonList(toRedisAccountsKey(accountId)),
        // Arg1: amount
        amount + "",
        // Arg2: idempotency_key
        idempotencyKey
      );

      logger.debug(
        "Processed Incoming Settlement Amount: `{}`. AccountId `{}` has new clearing_balance " +
          "(including prepaid amount): `{}`", amount, accountId, result
      );
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error handling Incoming Settlement from Settlement Engine with amount `%s` for accountId `%s`",
        amount, accountId
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updateBalanceForOutgoingSettlementRefund(AccountId accountId, long amount) throws BalanceTrackerException {
    Objects.requireNonNull(accountId, "accountId must not be null");

    // See note in updateBalanceForPrepare for why this is not using unsigned longs
    Preconditions.checkArgument(
      amount >= 0,
      String.format("amount `%s` must be a positive signed long!", amount)
    );

    try {
      long newClearingBalance = stringRedisTemplate.execute(
        updateBalanceForSettlementRefundScript,
        singletonList(toRedisAccountsKey(accountId)),
        // Arg1: amount
        amount + ""
      );

      logger.debug(
        "Processed Settlement Refund balance update with amount: `{}`. AccountId `{}` has new clearing_balance: `{}`",
        amount, accountId, newClearingBalance
      );

    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error attempting to refund settlement payment in Redis for accountId `%s` and amount `%s`",
        accountId, amount
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  /**
   * Helper method to convert an {@link AccountId} into a {@link String} for usage by Redis.
   *
   * @param accountId
   *
   * @return
   */
  private String toRedisAccountsKey(final AccountId accountId) {
    return "accounts:" + accountId.value();
  }

  /**
   * Helper method to convert a {@link String} into a long number.
   *
   * @param numAsString a {@code String} containing the {@code long} representation to be parsed
   *
   * @return the {@code long} represented by the argument in decimal.
   *
   * @throws NumberFormatException if the string does not contain a parsable {@code long}.
   */
  private long toLong(final String numAsString) {
    if (numAsString == null || numAsString.length() == 0) {
      return 0L;
    } else {
      // See note in updateBalanceForPrepare for why this is not Long.parseUnsignedLong
      return Long.parseLong(numAsString);
    }
  }
}
