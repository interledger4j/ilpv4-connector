package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class AccountSettlementEngineAlreadyExistsProblem extends AccountProblem {

  public AccountSettlementEngineAlreadyExistsProblem(final AccountId accountId, final String settlementEngineId) {
    super(
        URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/account-settlement-engine-already-exists"),
        "Account Settlement Engine Already Exists [accountId: `" + accountId + "`, settlementEngineId: `" +
            settlementEngineId + "`]",
        Status.CONFLICT,
        Objects.requireNonNull(accountId)
    );
  }
}
