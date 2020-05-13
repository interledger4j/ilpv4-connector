package org.interledger.connector.payments;

import org.interledger.connector.events.InvoiceCreateEvent;

/**
 * <p>Defines the Connector's view of the OpenPayments layer, allowing the Connector to interact with and trigger
 * functionality in the OpenPayments server.</p>
 *
 * <p>This facade provides an abstraction between the Connector and any OpenPayments-enabled systems that the two
 * system to operate together in a consistent manner.</p>
 */
public interface OpenPaymentsEventHandler {

  /**
   * Called when an invoice has been created in the OpenPayments layer.
   *
   * @param invoiceCreateEvent An {@link InvoiceCreateEvent} triggered by the OpenPayments server.
   */
  void onInvoiceCreated(InvoiceCreateEvent invoiceCreateEvent);
}
