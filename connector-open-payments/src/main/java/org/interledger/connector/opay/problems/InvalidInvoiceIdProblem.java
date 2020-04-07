package org.interledger.connector.opay.problems;

import static org.interledger.connector.opay.problems.InvoiceProblem.INVOICES_PATH;

import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

public class InvalidInvoiceIdProblem extends AbstractConnectorProblem {

  public InvalidInvoiceIdProblem(final String detail) {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-id"),
      "Invalid InvoiceId",
      Status.BAD_REQUEST,
      detail
    );
  }
}
