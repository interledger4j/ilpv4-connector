package org.interledger.connector.transactions;

public class InDatabasePaymentTransactionAggregator implements PaymentTransactionAggregator {

  private final PaymentTransactionManager paymentTransactionManager;

  public InDatabasePaymentTransactionAggregator(PaymentTransactionManager paymentTransactionManager) {
    this.paymentTransactionManager = paymentTransactionManager;
  }

  @Override
  public void aggregate(Transaction transaction) {
    paymentTransactionManager.upsert(transaction);
  }

}
