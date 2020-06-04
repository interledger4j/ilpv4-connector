package org.interledger.openpayments.problems;


import org.interledger.openpayments.InvoiceId;

import org.zalando.problem.Status;

import java.net.URI;
import java.util.Objects;

public class InvoicePaymentProblem extends InvoiceProblem {

  public InvoicePaymentProblem(final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-payment-problem"),
      "Payment failed for invoice with id = " + invoiceId.value(),
      Status.INTERNAL_SERVER_ERROR,
      Objects.requireNonNull(invoiceId)
    );
  }

  public InvoicePaymentProblem(final String detail, final InvoiceId invoiceId) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invoice-payment-problem"),
      "Payment failed for invoice with id = " + invoiceId.value(),
      Status.INTERNAL_SERVER_ERROR,
      detail,
      Objects.requireNonNull(invoiceId)
    );
  }
}
