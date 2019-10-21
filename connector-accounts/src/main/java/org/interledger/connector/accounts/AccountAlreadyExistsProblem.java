package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown on a request to create account for an already existing account id
 */
public class AccountAlreadyExistsProblem extends AccountProblem {

  public AccountAlreadyExistsProblem(final AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/account-already-exists"),
      "Account Already Exists (`" + accountId + "`)",
      Status.CONFLICT,
      Objects.requireNonNull(accountId)
    );
  }

}
