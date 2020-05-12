package org.interledger.connector.payments;

/**
 * Service for initiating send payments for accounts on this connector.
 */
public interface SendPaymentService {

  StreamPayment sendMoney(SendPaymentRequest request);

}
