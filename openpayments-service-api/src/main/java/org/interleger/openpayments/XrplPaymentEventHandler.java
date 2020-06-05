package org.interleger.openpayments;


import org.interledger.openpayments.events.XrpPaymentCompletedEvent;

/**
 * Defines the OpenPayments view of a "payment system" allowing the OpenPayments server to interact with an underlying
 * payment system (e.g., an ILP Connector).
 *
 * This facade provides an abstraction between the OpenPayments services and any underlying payment service in order to
 * enable the OpenPayments server to interoperate with non-Connector payment layers in a consistent manner.
 */
public interface XrplPaymentEventHandler {

  /**
   * Called whenever a new {@link XrpPaymentCompletedEvent} is encountered. Implementations should implement this method to
   * handle this event properly.
   */
  void onPaymentCompleted(XrpPaymentCompletedEvent xrpPaymentCompletedEvent);

}
