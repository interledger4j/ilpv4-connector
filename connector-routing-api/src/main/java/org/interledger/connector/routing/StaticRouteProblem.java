package org.interledger.connector.routing;

import org.interledger.connector.core.problems.AbstractConnectorProblem;
import org.interledger.core.InterledgerAddressPrefix;

import org.zalando.problem.StatusType;

import java.net.URI;
import java.util.Objects;

/**
 * A root exception for all exceptions relating to static routes.
 */
public abstract class StaticRouteProblem extends AbstractConnectorProblem {

  protected static final String STATIC_ROUTES_PATH = "/routes/static";

  private final InterledgerAddressPrefix prefix;

  public StaticRouteProblem(URI type, String title, StatusType status, InterledgerAddressPrefix prefix) {
    super(type, title, status);
    this.prefix = Objects.requireNonNull(prefix, "prefix must not be null!");
  }

  public StaticRouteProblem(URI type, String title, StatusType status, String detail, InterledgerAddressPrefix prefix) {
    super(type, title, status, detail);
    this.prefix = Objects.requireNonNull(prefix, "prefix must not be null!");
  }

  /**
   * @return the prefix that caused the associated problem
   */
  public InterledgerAddressPrefix getPrefix() {
    return this.prefix;
  }
}
