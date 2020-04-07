package org.interledger.connector.transactions;

/**
 * Status of a {@link Transaction}.
 */
public enum TransactionStatus {

  UNKNOWN, // when transaction status from the database can't be mapped
  PENDING, // for transactions with packets in flight that haven't closed yet
  CLOSED_BY_EXPIRATION, // indicates transaction was closed as a result of max transaction time expiration
  CLOSED_BY_STREAM; // indicates transaction was closed as a result of sender sending a StreamClose frame

}
