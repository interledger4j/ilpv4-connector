package org.interledger.connector.accounts;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Thrown if supplied account details are not configured properly.
 */
public class InvalidAccountDetailsProblem extends AccountProblem {

  public InvalidAccountDetailsProblem(final AccountId accountId, final String details) {
    super(
      URI.create(TYPE_PREFIX + ACCOUNTS_PATH + "/invalid-account-details"),
      "Invalid Account Details",
      Status.BAD_REQUEST,
      Objects.requireNonNull(accountId),
      details
    );
  }
}
