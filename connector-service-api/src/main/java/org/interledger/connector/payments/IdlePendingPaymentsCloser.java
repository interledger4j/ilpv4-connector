package org.interledger.connector.payments;

import org.interledger.connector.events.StreamPaymentClosedEvent;

/**
 * Service for closing pending {@link StreamPayment} that have been idle (i.e. no recent activity)
 * and emitting {@link StreamPaymentClosedEvent} for each closed payment.
 */
public interface IdlePendingPaymentsCloser {

  void closeExpiredPayments();

}
