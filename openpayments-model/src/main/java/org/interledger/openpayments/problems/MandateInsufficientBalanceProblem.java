package org.interledger.openpayments.problems;

import org.interledger.openpayments.MandateId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

/**
 * Problem that indicates mandates could not be created because there were insufficient balance on the mandate
 * to cover the mandate. This would happen if a mandate has already been mandated the maximum amount and an attempt
 * is made for additional mandates.
 */
public class MandateInsufficientBalanceProblem extends MandateProblem {
  public MandateInsufficientBalanceProblem(final MandateId mandateId) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-insufficient-balance"),
      "Mandate does not have sufficient balance for charge (" + mandateId.value() + ")",
      Status.CONFLICT,
      Objects.requireNonNull(mandateId)
    );
  }

  public MandateInsufficientBalanceProblem(final HttpUrl mandateUrl) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-insufficient-balance"),
      "Mandate does not have sufficient balance for charge (" + mandateUrl + ")",
      Status.CONFLICT,
      Objects.requireNonNull(mandateUrl)
    );
  }

  public MandateInsufficientBalanceProblem(final String detail, final MandateId mandateId) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-insufficient-balance"),
      "Mandate does not have sufficient balance for charge (" + mandateId.value() + ")",
      Status.CONFLICT,
      detail,
      Objects.requireNonNull(mandateId)
    );
  }

  public MandateInsufficientBalanceProblem(final String detail, final HttpUrl mandateUrl) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-insufficient-balance"),
      "Mandate does not have sufficient balance for charge (" + mandateUrl + ")",
      Status.CONFLICT,
      detail,
      Objects.requireNonNull(mandateUrl)
    );
  }
}
