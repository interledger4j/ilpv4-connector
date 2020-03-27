package org.interledger.connector.problems.spsp;

import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

/**
 * A root exception for all exceptions relating to ILPv4 Accounts.
 */
public class InvalidSpspRequestProblem extends AbstractConnectorProblem {

  protected static final String SPSP_PATH = "/spsp";

  public InvalidSpspRequestProblem() {
    super(
      URI.create(TYPE_PREFIX + SPSP_PATH + "/invalid-request"),
      "Invalid SPSP Request",
      Status.BAD_REQUEST
    );
  }
}
