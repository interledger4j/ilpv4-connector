package org.interledger.connector.opa.model.problems;

import org.interledger.connector.core.problems.AbstractConnectorProblem;
import org.interledger.connector.opa.model.MandateId;

import okhttp3.HttpUrl;
import org.zalando.problem.StatusType;

import java.net.URI;

/**
 * A root exception for all exceptions relating to mandates.
 */
public class MandateProblem extends AbstractConnectorProblem {

  public static final String MANDATES_PATH = "/mandates";

  /**
   * The mandate id of the mandate that threw this exception.
   */
  private final MandateId mandateId;

  private final HttpUrl mandateUrl;

  public MandateProblem(URI type, String title, StatusType status, MandateId mandateId) {
    super(type, title, status);
    this.mandateId = mandateId;
    this.mandateUrl = null;
  }

  public MandateProblem(URI type, String title, StatusType status, String detail, MandateId mandateId) {
    super(type, title, status, detail);
    this.mandateId = mandateId;
    this.mandateUrl = null;
  }

  public MandateProblem(URI type, String title, StatusType status, HttpUrl mandateUrl) {
    super(type, title, status);
    this.mandateUrl = mandateUrl;
    this.mandateId = null;
  }

  public MandateProblem(URI type, String title, StatusType status, String detail, HttpUrl mandateUrl) {
    super(type, title, status, detail);
    this.mandateUrl = mandateUrl;
    this.mandateId = null;
  }

  /**
   * @return the {@link MandateId} of the mandate that threw this exception.
   */
  public MandateId getMandateId() {
    return this.mandateId;
  }
}
