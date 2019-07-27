package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown if a settlement operation is attempted on an account with no Settlement Engine configured.
 */
public class SettlementEngineNotConfiguredProblem extends AccountProblem {

  public SettlementEngineNotConfiguredProblem(final AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/settlement-engine-not-configured"),
      "Settlement Engine Not Configured",
      Status.BAD_REQUEST,
      Objects.requireNonNull(accountId)
    );
  }
}
