package org.interledger.connector.opa;


import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.payments.StreamPayment;

import java.util.Optional;

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

  Invoice updateInvoice(final Invoice invoice);

  /**
   * Execute any actions necessary in the event of a received XRP payment.
   *
   * @param xrpPayment An {@link XrpPayment} with details about the XRP payment.
   * @return The updated invoice, if the XRP payment was determined to be for an invoice, otherwise empty.
   */
  Optional<Invoice> onPayment(final XrpPayment xrpPayment);

  /**
   * Execute any actions necessary in the event of a received ILP STREAM payment.
   *
   * @param streamPayment A {@link StreamPayment} with details about the ILP payment.
   * @return The updated invoice, if the ILP payment was determined to be for an invoice, otherwise empty.
   */
  Optional<Invoice> onPayment(final StreamPayment streamPayment);
}
