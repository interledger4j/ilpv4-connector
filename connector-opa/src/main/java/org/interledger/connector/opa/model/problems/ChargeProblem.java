package org.interledger.connector.opa.model.problems;

import org.interledger.connector.core.problems.AbstractConnectorProblem;
import org.interledger.connector.opa.model.ChargeId;

import okhttp3.HttpUrl;
import org.zalando.problem.StatusType;

import java.net.URI;

/**
 * A root exception for all exceptions relating to charges.
 */
public class ChargeProblem extends AbstractConnectorProblem {

  public static final String CHARGES_PATH = "/charges";

  /**
   * The charge id of the charge that threw this exception.
   */
  private final ChargeId chargeId;

  private final HttpUrl chargeUrl;

  public ChargeProblem(URI type, String title, StatusType status, ChargeId chargeId) {
    super(type, title, status);
    this.chargeId = chargeId;
    this.chargeUrl = null;
  }

  public ChargeProblem(URI type, String title, StatusType status, String detail, ChargeId chargeId) {
    super(type, title, status, detail);
    this.chargeId = chargeId;
    this.chargeUrl = null;
  }

  public ChargeProblem(URI type, String title, StatusType status, HttpUrl chargeUrl) {
    super(type, title, status);
    this.chargeUrl = chargeUrl;
    this.chargeId = null;
  }

  public ChargeProblem(URI type, String title, StatusType status, String detail, HttpUrl chargeUrl) {
    super(type, title, status, detail);
    this.chargeUrl = chargeUrl;
    this.chargeId = null;
  }

  /**
   * @return the {@link ChargeId} of the charge that threw this exception.
   */
  public ChargeId getChargeId() {
    return this.chargeId;
  }
}
