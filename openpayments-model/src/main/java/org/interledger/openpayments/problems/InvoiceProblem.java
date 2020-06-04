package org.interledger.openpayments.problems;

import org.interledger.openpayments.InvoiceId;

import okhttp3.HttpUrl;
import org.zalando.problem.StatusType;

import java.net.URI;

/**
 * A root exception for all exceptions relating to invoices.
 */
public class InvoiceProblem extends AbstractOpenPaymentsProblem {

  public static final String INVOICES_PATH = "/invoices";

  /**
   * The invoice id of the invoice that threw this exception.
   */
  private final InvoiceId invoiceId;

  private final HttpUrl invoiceUrl;

  public InvoiceProblem(URI type, String title, StatusType status, InvoiceId invoiceId) {
    super(type, title, status);
    this.invoiceId = invoiceId;
    this.invoiceUrl = null;
  }

  public InvoiceProblem(URI type, String title, StatusType status, String detail, InvoiceId invoiceId) {
    super(type, title, status, detail);
    this.invoiceId = invoiceId;
    this.invoiceUrl = null;
  }

  public InvoiceProblem(URI type, String title, StatusType status, HttpUrl invoiceUrl) {
    super(type, title, status);
    this.invoiceUrl = invoiceUrl;
    this.invoiceId = null;
  }

  public InvoiceProblem(URI type, String title, StatusType status, String detail, HttpUrl invoiceUrl) {
    super(type, title, status, detail);
    this.invoiceUrl = invoiceUrl;
    this.invoiceId = null;
  }

  /**
   * @return the {@link InvoiceId} of the invoice that threw this exception.
   */
  public InvoiceId getInvoiceId() {
    return this.invoiceId;
  }
}
