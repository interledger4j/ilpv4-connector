package org.interledger.connector.opa.service;


import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;

/**
 * Service layer interface for dealing with {@link Invoice}s in different Open Payments flows.
 */
public interface InvoiceService {

  /**
   * Get an existing invoice.
   *
   * @param invoiceId The {@link InvoiceId} of the invoice to get.
   * @return The existing {@link Invoice} with the specified {@link InvoiceId}
   */
  Invoice getInvoiceById(final InvoiceId invoiceId);

  /**
   * Create a new invoice by storing it.
   *
   * @param invoice The {@link Invoice} to store.
   * @return The {@link Invoice} that was stored.
   */
  Invoice createInvoice(final Invoice invoice);

  /**
   * Update an existing invoice.
   *
   * @param invoice An {@link Invoice} with a non-null {@code Invoice#invoiceId} and some changed data.
   * @return The updated invoice.
   */
  Invoice updateInvoice(final Invoice invoice);

  /**
   * Get the payment address of an invoice subject in order to pay an invoice.
   *
   * For ILP payments, this will be an ILP address.  For XRP payments, this will be an XRP address.
   *
   * @param subject The subject of the invoice.
   * @return The address at which a sender can send money to pay off an invoice.
   */
  String getAddressFromInvoiceSubject(final String subject);
}
