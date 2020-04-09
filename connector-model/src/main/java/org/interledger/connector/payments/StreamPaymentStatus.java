package org.interledger.connector.payments;

/**
 * Status of a {@link StreamPayment}.
 */
public enum StreamPaymentStatus {

  PENDING, // for stream payments with packets in flight that haven't closed yet
  CLOSED_BY_EXPIRATION, // indicates stream payment was closed as a result of max stream payment time expiration
  CLOSED_BY_STREAM; // indicates stream payment was closed as a result of sender sending a StreamClose frame

}
