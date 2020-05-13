package org.interledger.connector.payments;

/**
 * <p>Defines the Connector's view of the OpenPayments layer, allowing the Connector to interact with and trigger
 * functionality in the OpenPayments server.</p>
 *
 * <p>This facade provides an abstraction between the Connector and any OpenPayments-enabled systems that the two
 * system to operate together in a consistent manner.</p>
 */
public interface OpenPaymentsFacade {

  /**
   * Called by the connector when an payment has completed.
   *
   * @param streamPayment A {@link StreamPayment} that has completed.
   */
  void emitPaymentCompleted(StreamPayment streamPayment);
}
