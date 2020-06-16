package org.interledger.openpayments.problems;

import static org.interledger.openpayments.problems.InvoiceProblem.INVOICES_PATH;

import org.interledger.openpayments.InvoiceId;
import org.interledger.openpayments.PayIdAccountId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvalidPayIdAccountIdProblem extends PayIdAccountIdProblem {

  public InvalidPayIdAccountIdProblem(final String detail) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-payId-accountId"),
      "Invalid PayIdAccountId.",
      Status.BAD_REQUEST,
      detail
    );
  }
}
