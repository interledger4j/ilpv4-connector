package org.interledger.connector.accounts;

import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

public class InvalidEncryptionRequestProblem extends AbstractConnectorProblem {

  public InvalidEncryptionRequestProblem(String path, String message) {
    super(URI.create(TYPE_PREFIX + path + "/invalid-encryption-request"),
      message,
      Status.BAD_REQUEST);
  }
}
