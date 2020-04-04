package org.interledger.connector.transactions;

public interface FulfilledTransactionAggregator {

  void aggregate(Transaction transaction);

}
