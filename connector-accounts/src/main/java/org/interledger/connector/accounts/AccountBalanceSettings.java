package org.interledger.connector.accounts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * <p>Defines whether a node (e.g., a Connector) should maintain and enforce a balance for this account. The balance
 * is always from the node's perspective. Therefore, a negative balance implies the node owes money to the counterparty
 * and a positive balance implies the counterparty owes money to the node.</p>
 *
 * <p>This object is optionally-present on an account, since some accounts do not track balances (e.g., client
 * accounts). Alternatively, even if an account does track a balance, it may be desirable to not have any caps.</p>
 */
public interface AccountBalanceSettings {

  static ImmutableAccountBalanceSettings.Builder builder() {
    return ImmutableAccountBalanceSettings.builder();
  }

  /**
   * <p>The minimum balance (in this account's indivisible base units) the connector must maintain for this account.
   * For example, the connector will reject incoming packets if the transaction would put the account balance below this
   * number. If this value is not present, then the connector will assume no minimum balance.</p>
   *
   * <p>Note that it is permissible to use a {@link Long} here since this value represents a minimum value, which is
   * sign-less. Thus, we can use the largest long to represent the largest minimum balance.</p>
   *
   * @return The minimum balance, or {@link Optional#empty()} if there is no minimum.
   */
  Optional<Long> minBalance();

  /**
   * <p>Optional Balance (in this account's indivisible base units) numerically below which the connector will
   * automatically initiate a settlement.</p>
   *
   * <p>Note that it is permissible to use a {@link Long} here since this value represents a threshold for a value
   * that will never go negative (this roughly correlates to the `prepaid_balance` in the balance tracker, and this will
   * never go negative). Thus, we can use the largest long number to represent the largest threshold).</p>
   *
   * @return The settlement threshold balance, or {@link Optional#empty()} if there is no threshold (i.e., the account
   * should never settle).
   */
  Optional<Long> settleThreshold();

  /**
   * <p>The account balance (in this account's indivisible base units) the connector will attempt to reach when
   * settling (default value is 0).</p>
   *
   * <p>Note that it is permissible to use a {@link long} here since this value only ever represents a positive
   * unsigned long number or zero.</p>
   *
   * @return The amount that triggers settlement.
   */
  default long settleTo() {
    return 0L;
  }

  @Value.Immutable(intern = true)
  @Value.Modifiable
  @JsonSerialize(as = ImmutableAccountBalanceSettings.class)
  @JsonDeserialize(as = ImmutableAccountBalanceSettings.class)
  abstract class AbstractAccountBalanceSettings implements AccountBalanceSettings {

    @Override
    @Value.Default
    public long settleTo() {
      return 0L;
    }

    @Value.Check
    public AbstractAccountBalanceSettings check() {
      this.settleThreshold()
        .ifPresent(settleThreshold -> Preconditions.checkArgument(settleThreshold >= settleTo(),
          "settleThreshold must be greater than or equal to the settleTo"
        ));
      return this;
    }

  }
}
