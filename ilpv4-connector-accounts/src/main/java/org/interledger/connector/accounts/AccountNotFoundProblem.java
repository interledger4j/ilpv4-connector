package org.interledger.connector.accounts;

import org.zalando.problem.Status;
import org.zalando.problem.StatusType;

import java.net.URI;

/**
 * Thrown if an account is not found.
 */
public class AccountNotFoundProblem extends AccountProblem {

  public AccountNotFoundProblem(URI type, String title, StatusType status, AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/account-not-found"),
      "Account Not Found",
      Status.NOT_FOUND,
      accountId
    );
  }
}
