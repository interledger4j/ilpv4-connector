package org.interledger.connector.opa.model.problems;

import org.interledger.connector.opa.model.InvoiceId;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvalidInvoiceSubjectProblem extends InvoiceProblem {
  public InvalidInvoiceSubjectProblem(final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-subject"),
      "Invalid Invoice Subject (" + invoiceId.value() + ")",
      Status.BAD_REQUEST,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvalidInvoiceSubjectProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-subject"),
      "Invalid Invoice Subject (" + invoiceId.value() + ")",
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }
}
