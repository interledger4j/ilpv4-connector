package org.interledger.connector.core.problems;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Map;

/**
 * The root class for all Problems that this Connector might emit.
 */
public class AbstractConnectorProblem extends AbstractThrowableProblem {

  // Use a sub-domain to avoid accidentally DOSing the root domain.
  public static final String TYPE_PREFIX = "https://errors.interledger.org";

  protected AbstractConnectorProblem() {
    super();
  }

  protected AbstractConnectorProblem(@Nullable URI type) {
    super(type);
  }

  protected AbstractConnectorProblem(@Nullable URI type, @Nullable String title) {
    super(type, title);
  }

  protected AbstractConnectorProblem(@Nullable URI type, @Nullable String title, @Nullable StatusType status) {
    super(type, title, status);
  }

  protected AbstractConnectorProblem(
    @Nullable URI type, @Nullable String title, @Nullable StatusType status, @Nullable String detail) {
    super(type, title, status, detail);
  }

  protected AbstractConnectorProblem(
    @Nullable URI type,
    @Nullable String title, @Nullable StatusType status, @Nullable String detail, @Nullable URI instance) {
    super(type, title, status, detail, instance);
  }

  protected AbstractConnectorProblem(
    @Nullable URI type,
    @Nullable String title,
    @Nullable StatusType status, @Nullable String detail, @Nullable URI instance, @Nullable ThrowableProblem cause) {
    super(type, title, status, detail, instance, cause);
  }

  protected AbstractConnectorProblem(
    @Nullable URI type,
    @Nullable String title,
    @Nullable StatusType status,
    @Nullable String detail,
    @Nullable URI instance, @Nullable ThrowableProblem cause, @Nullable Map<String, Object> parameters) {
    super(type, title, status, detail, instance, cause, parameters);
  }
}
