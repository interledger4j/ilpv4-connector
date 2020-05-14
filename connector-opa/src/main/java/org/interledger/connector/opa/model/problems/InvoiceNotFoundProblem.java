package org.interledger.connector.opa.model.problems;

import org.interledger.connector.opa.model.InvoiceId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvoiceNotFoundProblem extends InvoiceProblem {
  public InvoiceNotFoundProblem(final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-not-found"),
      "Invoice not found (" + invoiceId.value() + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvoiceNotFoundProblem(final HttpUrl invoiceUrl) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-not-found"),
      "Invoice not found (" + invoiceUrl + ")",
      Status.NOT_FOUND,
      Objects.requireNonNull(invoiceUrl)
    );
  }

  public InvoiceNotFoundProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-not-found"),
      "Invoice not found (" + invoiceId.value() + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvoiceNotFoundProblem(final String detail, final HttpUrl invoiceUrl) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-not-found"),
      "Invoice not found (" + invoiceUrl + ")",
      Status.NOT_FOUND,
      detail,
      Objects.requireNonNull(invoiceUrl)
    );
  }
}
