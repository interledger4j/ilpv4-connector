package org.interledger.openpayments.problems;

import org.interledger.openpayments.MandateId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class MandateNotFoundProblem extends MandateProblem {
  public MandateNotFoundProblem(final MandateId mandateId) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-found"),
      "Mandate not found (" + mandateId.value() + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(mandateId)
    );
  }

  public MandateNotFoundProblem(final HttpUrl mandateUrl) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-found"),
      "Mandate not found (" + mandateUrl + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(mandateUrl)
    );
  }

  public MandateNotFoundProblem(final String detail, final MandateId mandateId) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-found"),
      "Mandate not found (" + mandateId.value() + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(mandateId)
    );
  }

  public MandateNotFoundProblem(final String detail, final HttpUrl mandateUrl) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-found"),
      "Mandate not found (" + mandateUrl + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(mandateUrl)
    );
  }
}
