package org.interledger.connector.server.spring.controllers.model.problems;

import com.google.common.collect.ImmutableMap;
import org.interledger.connector.accounts.AccountId;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

public class AccountNotFoundProblem extends AbstractThrowableProblem {

  private static final URI TYPE = URI.create("https://interledger.org/connector/account-not-found");

  public AccountNotFoundProblem(final AccountId accountId) {
    super(TYPE, "Account Not Found", Status.NOT_FOUND, null, null, null,
      ImmutableMap.<String, Object>builder().put("accountId", accountId).build());
  }
}
