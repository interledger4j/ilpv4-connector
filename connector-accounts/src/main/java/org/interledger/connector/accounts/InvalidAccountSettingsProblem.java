package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown if an account is not found.
 */
public class InvalidAccountSettingsProblem extends AccountProblem {

  public InvalidAccountSettingsProblem(final AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/invalid-account-settings"),
      "Invalid Account Settings",
      Status.BAD_REQUEST,
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
  public InvalidAccountSettingsProblem(final String detail, final AccountId accountId) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/invalid-account-settings"),
      "Invalid Account Settings",
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(accountId)
    );
  }


}
