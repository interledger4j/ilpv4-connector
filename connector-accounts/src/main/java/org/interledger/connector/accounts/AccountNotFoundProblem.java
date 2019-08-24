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
      "Account Not Found (`" + accountId + "`)",
      Status.NOT_FOUND,
      Objects.requireNonNull(accountId)
    );
  }

  /**
   * Required-args Constructor.
   *
   * @param detail    A {@link String} containing a more customized error message for internal server logging purposes
   *                  only (i.e., this value will not affect the problem's `detail` message).
   * @param accountId The {@link AccountId} that was unable to be found.
   */
  public AccountNotFoundProblem(final String detail, final AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/account-not-found"),
      "Account Not Found (`" + accountId + "`)",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(accountId)
    );
  }


}
