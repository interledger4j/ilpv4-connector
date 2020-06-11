package org.interledger.openpayments.problems;

import org.interledger.openpayments.MandateId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class MandateNotApprovedProblem extends MandateProblem {
  public MandateNotApprovedProblem(final MandateId mandateId) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-approved"),
      "Mandate not approved (" + mandateId.value() + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(mandateId)
    );
  }

  public MandateNotApprovedProblem(final HttpUrl mandateUrl) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-approved"),
      "Mandate not approved (" + mandateUrl + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(mandateUrl)
    );
  }

  public MandateNotApprovedProblem(final String detail, final MandateId mandateId) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-approved"),
      "Mandate not approved (" + mandateId.value() + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(mandateId)
    );
  }

  public MandateNotApprovedProblem(final String detail, final HttpUrl mandateUrl) {
    super(
      URI.create(TYPE_PREFIX + MANDATES_PATH + "/mandate-not-approved"),
      "Mandate not approved (" + mandateUrl + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(mandateUrl)
    );
  }
}
