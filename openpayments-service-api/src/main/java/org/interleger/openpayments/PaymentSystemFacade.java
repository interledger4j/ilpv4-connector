package org.interleger.openpayments;

import org.interledger.connector.opa.model.Invoice;

/**
 * <p>Defines the OpenPayments view of a "payment system" allowing the OpenPayments server to send messages/events to
 * any underlying payment system (e.g., an ILP Connector).</p>
 */
public interface PaymentSystemFacade {

  /**
   * Called by the OpenPayments server to obtain a payment address for the specified invoice.
   *
   * @param invoice An {@link Invoice}.
   *
   * @return
   */
  String getPaymentAddress(Invoice invoice);
}
