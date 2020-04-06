package org.interledger.connector.transactions;

public interface PaymentTransactionAggregator {

  void aggregate(Transaction transaction);

}
