package org.interledger.connector.opa.model.problems;

import org.interledger.connector.core.problems.AbstractConnectorProblem;
import org.interledger.connector.opa.model.InvoiceId;

import org.zalando.problem.StatusType;

import java.net.URI;
import javax.annotation.Nullable;

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

  public InvoiceProblem(URI type, String title, StatusType status, String detail, InvoiceId invoiceId) {
    super(type, title, status, detail);
    this.invoiceId = invoiceId;
  }

  /**
   * @return the {@link InvoiceId} of the invoice that threw this exception.
   */
  public InvoiceId getInvoiceId() {
    return this.invoiceId;
  }
}
