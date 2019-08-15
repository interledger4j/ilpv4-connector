package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown if an account is not found by its settlement engine id.
 */
public class AccountBySettlementEngineAccountNotFoundProblem extends AccountProblem {

  /**
   * The id of the settlement account identifier used to lookup an account.
   */
  private final SettlementEngineAccountId settlementEngineAccountId;

  public AccountBySettlementEngineAccountNotFoundProblem(final SettlementEngineAccountId settlementEngineAccountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/account-not-found"),
      "Account Not Found by settlementEngineAccountId (`" + settlementEngineAccountId + "`)",
      Status.NOT_FOUND,
      Objects.requireNonNull(AccountId.of("n/a"))
    );

    this.settlementEngineAccountId = Objects.requireNonNull(settlementEngineAccountId);
  }

  /**
   * The {@link SettlementEngineAccountId} of the account that threw this exception.
   */
  public SettlementEngineAccountId getSettlementEngineAccountId() {
    return settlementEngineAccountId;
  }

}
