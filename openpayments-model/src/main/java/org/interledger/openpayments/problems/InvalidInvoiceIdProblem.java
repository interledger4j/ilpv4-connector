package org.interledger.openpayments.problems;


import org.interledger.openpayments.Ids;
import org.interledger.openpayments.InvoiceId;

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

  public InvalidInvoiceIdProblem(final String detail, final Ids._InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-id"),
      "Invalid InvoiceId (" + invoiceId.value() + ")",
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(InvoiceId.of(invoiceId.value()))
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
