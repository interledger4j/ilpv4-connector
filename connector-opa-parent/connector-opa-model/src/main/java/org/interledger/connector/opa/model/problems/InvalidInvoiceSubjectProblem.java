package org.interledger.connector.opa.model.problems;

import static org.interledger.connector.opa.model.problems.InvoiceProblem.INVOICES_PATH;

import org.interledger.connector.core.problems.AbstractConnectorProblem;

import org.zalando.problem.Status;

import java.net.URI;

public class InvalidInvoiceSubjectProblem extends AbstractConnectorProblem {
  public InvalidInvoiceSubjectProblem() {
    super(
      URI.create(TYPE_PREFIX + INVOICES_PATH + "/invalid-invoice-subject"),
      "Invalid Invoice Subject",
      Status.BAD_REQUEST
    );
  }
}
