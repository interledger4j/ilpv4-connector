package org.interledger.connector.opa.model.problems;

import org.interledger.connector.opa.model.InvoiceId;

import okhttp3.HttpUrl;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvoiceAlreadyExistsProblem extends InvoiceProblem {
  public InvoiceAlreadyExistsProblem(final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-already-exists"),
      "Invoice already exists with id = " + invoiceId.value(),
      Status.CONFLICT,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvoiceAlreadyExistsProblem(final HttpUrl invoiceUrl) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-already-exists"),
      "Invoice already exists with url = " + invoiceUrl.toString(),
      Status.CONFLICT,
      Objects.requireNonNull(invoiceUrl)
    );
  }

  public InvoiceAlreadyExistsProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-already-exists"),
      "Invoice already exists with id = " + invoiceId.value(),
      Status.CONFLICT,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvoiceAlreadyExistsProblem(final String detail, final HttpUrl invoiceUrl) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-already-exists"),
      "Invoice already exists with url = " + invoiceUrl.toString(),
      Status.CONFLICT,
      detail,
      Objects.requireNonNull(invoiceUrl)
    );
  }
}
