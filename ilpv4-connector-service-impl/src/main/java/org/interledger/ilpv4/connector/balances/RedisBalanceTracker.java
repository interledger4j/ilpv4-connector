package org.interledger.ilpv4.connector.balances;

import com.sappenin.interledger.ilpv4.connector.balances.AccountBalance;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singletonList;

/**
 * An implementation of {@link BalanceTracker} that uses Redis to track all balances.
 */
public class RedisBalanceTracker implements BalanceTracker {

  // TODO: Stop sending Strings into Redis and instead send longs!
  // See https://github.com/sappenin/java-ilpv4-connector/issues/153

  public static final String BALANCE = "balance";
  public static final String PREPAID_AMOUNT = "prepaid_amount";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // The following are Lua scripts that are used to atomically execute the given logic inside Redis. This allows for
  // more complex logic without needing multiple round trips for messages to be sent to and from Redis, as well as
  // locks to ensure no other process is accessing Redis at the same time. Note that all scripts are injected so that
  // the SHA checksum is not recalculated on every execution.
  // For more information on scripting in Redis, see https://redis.io/commands/eval
  private final RedisScript<Long> updateBalanceForPrepareScript;
  private final RedisScript<Long> updateBalanceForFulfillScript;
  private final RedisScript<Long> updateBalanceForRejectScript;

  private final RedisTemplate<String, String> redisTemplate;

  public RedisBalanceTracker(
    final RedisScript<Long> updateBalanceForPrepareScript, final RedisScript<Long> updateBalanceForFulfillScript,
    final RedisScript<Long> updateBalanceForRejectScript,
    final RedisTemplate<String, String> redisTemplate
  ) {
    this.updateBalanceForPrepareScript = Objects.requireNonNull(updateBalanceForPrepareScript);
    this.updateBalanceForFulfillScript = Objects.requireNonNull(updateBalanceForFulfillScript);
    this.updateBalanceForRejectScript = Objects.requireNonNull(updateBalanceForRejectScript);
    this.redisTemplate = Objects.requireNonNull(redisTemplate);
  }

  @Override
  public AccountBalance getBalance(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    final BoundHashOperations<String, String, String> result =
      redisTemplate.boundHashOps(toRedisAccountsKey(accountId));

    return AccountBalance.builder()
      .accountId(accountId)
      .balance(toLong(result.get(BALANCE)))
      .prepaidAmount(toLong(result.get(PREPAID_AMOUNT)))
      .build();
  }

  @Override
  public void updateBalanceForPrepare(
    final AccountId sourceAccountId, final long sourceAmount, final Optional<Long> minBalance
  )
    throws BalanceTrackerException {

    Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null!");
    Objects.requireNonNull(sourceAmount, "sourceAmount must not be null!");
    Objects.requireNonNull(minBalance, "minBalance must not be null!");

    try {
      long result;
      if (minBalance.isPresent()) {
        result = redisTemplate.execute(
          updateBalanceForPrepareScript,
          singletonList(toRedisAccountsKey(sourceAccountId)),
          // Arg1: from_amount
          // Arg2: min_balance (optional)
          sourceAmount + "", minBalance.orElse(0L) + ""
        );
      } else {
        result = redisTemplate.execute(
          updateBalanceForPrepareScript,
          singletonList(toRedisAccountsKey(sourceAccountId)),
          // Arg1: from_amount
          sourceAmount + ""
        );
      }

      logger.debug(
        "Processed prepare with incoming amount: {}. Account {} has balance (including prepaid amount): {} ",
        sourceAmount, sourceAccountId, result
      );
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error handling prepare with sourceAmount `%s` from accountId `%s`", sourceAmount, sourceAccountId
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updateBalanceForFulfill(final AccountId destinationAccountId, final long destinationAmount) throws BalanceTrackerException {
    Objects.requireNonNull(destinationAccountId, "destinationAccountId must not be null!");
    Objects.requireNonNull(destinationAmount, "destinationAmount must not be null!");

    try {
      // TODO: Use BigDecimal
      Long result = redisTemplate.execute(
        updateBalanceForFulfillScript,
        singletonList(toRedisAccountsKey(destinationAccountId)),
        // Arg1: from_amount
        destinationAmount + ""
      );

      logger.debug(
        "Processed fulfill for outgoing amount: `{}`. Account `{}` has balance (including prepaid amount): `{}`",
        destinationAmount, destinationAccountId, result
      );
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error handling fulfill packet with destinationAmount `%s` from accountId `%s`", destinationAmount,
        destinationAccountId
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updateBalanceForReject(AccountId sourceAccountId, long sourceAmount) throws BalanceTrackerException {
    Objects.requireNonNull(sourceAccountId, "sourceAccountId must not be null!");
    Objects.requireNonNull(sourceAmount, "sourceAmount must not be null!");

    try {
      // TODO: Use BigDecimal
      long result = redisTemplate.execute(
        updateBalanceForRejectScript,
        singletonList(toRedisAccountsKey(sourceAccountId)),
        // Arg1: from_amount
        sourceAmount + ""
      );

      logger.debug(
        "Processed reject for incoming amount: `{}`. Account `{}` has balance (including prepaid amount): `{}`",
        sourceAmount, sourceAccountId, result
      );
    } catch (Exception e) {
      final String errorMessage = String.format(
        "Error handling reject packet with sourceAmount `%s` from accountId `%s`", sourceAmount, sourceAccountId
      );
      throw new BalanceTrackerException(errorMessage, e);
    }
  }

  private String toRedisAccountsKey(final AccountId accountId) {
    return "accounts:" + accountId.value();
  }

  private long toLong(final String numAsString) {
    if (numAsString == null || numAsString.length() == 0) {
      return 0L;
    } else {
      return Long.parseLong(numAsString);
    }
  }
}
