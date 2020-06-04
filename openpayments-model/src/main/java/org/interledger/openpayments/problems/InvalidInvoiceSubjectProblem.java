package org.interledger.openpayments.problems;

import static org.interledger.openpayments.problems.InvoiceProblem.INVOICES_PATH;

import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

public class InvalidInvoiceSubjectProblem extends AbstractConnectorProblem {
  public InvalidInvoiceSubjectProblem(final String invoiceSubject) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-subject"),
      "Invalid Invoice Subject (" + invoiceSubject + ")",
      Status.BAD_REQUEST,
      null
    );
  }

  public InvalidInvoiceSubjectProblem(final String detail, final String invoiceSubject) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-subject"),
      "Invalid Invoice Subject (" + invoiceSubject + ")",
      Status.BAD_REQUEST,
      detail
    );
  }
}
