package org.interleger.openpayments;

import org.interledger.connector.opa.model.Invoice;

/**
 * <p>Defines the OpenPayments view of a "payment system" allowing the OpenPayments server to send messages/events to
 * any underlying payment system (e.g., an ILP Connector).</p>
 */
public interface PaymentSystemFacade {

  /**
   * Called by the OpenPayments server when an invoice has been created.
   *
   * @param invoice A newly created {@link Invoice}.
   */
  void emitInvoiceCreated(Invoice invoice);

}
