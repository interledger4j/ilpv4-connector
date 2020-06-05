package org.interledger.openpayments.problems;


import org.interledger.openpayments.InvoiceId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvoicePaymentDetailsProblem extends InvoiceProblem {

  public InvoicePaymentDetailsProblem(final HttpUrl invoiceLocation, final int errorCode) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/payment-details"),
      String.format("Payment details request failed for {}. Error code = {}", invoiceLocation.toString(), errorCode),
      Status.BAD_REQUEST,
      Objects.requireNonNull(InvoiceId.of(invoiceLocation.pathSegments().get(invoiceLocation.pathSegments().size() - 1)))
    );
  }

  public InvoicePaymentDetailsProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/payment-details"),
      String.format("Payment details request failed for invoice %s", invoiceId),
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvoicePaymentDetailsProblem(final String detail, final HttpUrl invoiceLocation, final int errorCode) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/payment-details"),
      String.format("Payment details request failed for {}. Error code = {}", invoiceLocation.toString(), errorCode),
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(InvoiceId.of(invoiceLocation.pathSegments().get(invoiceLocation.pathSegments().size() - 1)))
    );
  }
}
