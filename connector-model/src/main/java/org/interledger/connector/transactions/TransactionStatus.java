package org.interledger.connector.transactions;

public enum TransactionStatus {

  UNKNOWN, // when transaction status from an unchec
  PENDING,
  CLOSED_BY_EXPIRATION, // indicates transaction was closed as a result of max transaction time expiration
  CLOSED_BY_STREAM; // indicates transaction was closed as a result of sender sending a StreamClose frame

}
