package org.interledger.openpayments.problems;

import org.interledger.openpayments.InvoiceId;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class UnsupportedInvoiceOperationProblem extends InvoiceProblem {

  public UnsupportedInvoiceOperationProblem(final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-operation-unsupported"),
      "Operation not supported on invoice " + invoiceId.value(),
      Status.BAD_REQUEST,
      Objects.requireNonNull(invoiceId)
    );
  }

  public UnsupportedInvoiceOperationProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-operation-unsupported"),
      "Operation not supported on invoice " + invoiceId.value(),
      Status.BAD_REQUEST,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }
}
