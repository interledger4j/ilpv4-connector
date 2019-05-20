package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown if an account is not found.
 */
public class AccountNotFoundProblem extends AccountProblem {

  public AccountNotFoundProblem(final AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/account-not-found"),
      "Account Not Found",
      Status.NOT_FOUND,
      Objects.requireNonNull(accountId)
    );
  }
}
