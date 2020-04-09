package org.interledger.connector.opa.model.problems;

import org.interledger.connector.opa.model.InvoiceId;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvalidInvoiceIdProblem extends InvoiceProblem {

  public InvalidInvoiceIdProblem(final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-id"),
      "Invalid InvoiceId (" + invoiceId.value() + ")",
      Status.BAD_REQUEST,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvalidInvoiceIdProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-id"),
      "Invalid InvoiceId (" + invoiceId.value() + ")",
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }
}
