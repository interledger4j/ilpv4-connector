package org.interledger.connector.accounts;

import static org.interledger.connector.accounts.AccountProblem.ACCOUNTS_PATH;

import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

/**
 * Thrown if an account's identifier is invalid.
 */
public class InvalidAccountIdProblem extends AbstractConnectorProblem {

  /**
   * Required-args Constructor.
   *
   * @param detail A {@link String} containing a more customized error message for user-facing error context.
   */
  public InvalidAccountIdProblem(final String detail) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/invalid-account-id"),
      "Invalid AccountId",
      Status.BAD_REQUEST,
      detail
    );
  }
}
