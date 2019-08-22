package org.interledger.connector.accounts;

import org.interledger.connector.core.problems.AbstractConnectorProblem;
import org.zalando.problem.StatusType;

import java.net.URI;
import java.util.Objects;

/**
 * A root exception for all exceptions relating to ILPv4 Accounts.
 */
public abstract class AccountProblem extends AbstractConnectorProblem {

  protected static final String ACCOUNTS_PATH = "/accounts";

  /**
   * The account-address of the account that threw this exception.
   */
  private final AccountId accountId;

  public AccountProblem(URI type, String title, StatusType status, AccountId accountId) {
    super(type, title, status);
    this.accountId = Objects.requireNonNull(accountId, "accountId must not be null!");
  }

  /**
   * The {@link AccountId} of the account that threw this exception.
   */
  public AccountId getAccountId() {
    return accountId;
  }

}
