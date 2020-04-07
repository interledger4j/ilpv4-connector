package org.interledger.connector.opay.problems;

import org.interledger.connector.core.problems.AbstractConnectorProblem;
import org.interledger.connector.opay.InvoiceId;

import org.zalando.problem.StatusType;

import java.net.URI;

/**
 * A root exception for all exceptions relating to invoices.
 */
public class InvoiceProblem extends AbstractConnectorProblem {

  public static final String INVOICES_PATH = "/invoices";

  /**
   * The invoice id of the invoice that threw this exception.
   */
  private final InvoiceId invoiceId;

  public InvoiceProblem(URI type, String title, StatusType status, InvoiceId invoiceId) {
    super(type, title, status);
    this.invoiceId = invoiceId;
  }

  /**
   * @return the {@link InvoiceId} of the invoice that threw this exception.
   */
  public InvoiceId getInvoiceId() {
    return this.invoiceId;
  }
}
