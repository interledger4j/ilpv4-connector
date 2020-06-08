package org.interledger.openpayments.problems;

import org.interledger.openpayments.ChargeId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class ChargeNotFoundProblem extends ChargeProblem {
  public ChargeNotFoundProblem(final ChargeId chargeId) {
    super(
      URI.create(TYPE_PREFIX + CHARGES_PATH + "/charge-not-found"),
      "Charge not found (" + chargeId.value() + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(chargeId)
    );
  }

  public ChargeNotFoundProblem(final HttpUrl chargeUrl) {
    super(
      URI.create(TYPE_PREFIX + CHARGES_PATH + "/charge-not-found"),
      "Charge not found (" + chargeUrl + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(chargeUrl)
    );
  }

  public ChargeNotFoundProblem(final String detail, final ChargeId chargeId) {
    super(
      URI.create(TYPE_PREFIX + CHARGES_PATH + "/charge-not-found"),
      "Charge not found (" + chargeId.value() + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(chargeId)
    );
  }

  public ChargeNotFoundProblem(final String detail, final HttpUrl chargeUrl) {
    super(
      URI.create(TYPE_PREFIX + CHARGES_PATH + "/charge-not-found"),
      "Charge not found (" + chargeUrl + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(chargeUrl)
    );
  }
}
