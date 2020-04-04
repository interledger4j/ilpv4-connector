package org.interledger.connector.transactions;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class InDatabaseFulfilledPacketTransactionAggregator implements FulfilledTransactionAggregator {

  private final PaymentTransactionManager paymentTransactionManager;

  public InDatabaseFulfilledPacketTransactionAggregator(PaymentTransactionManager paymentTransactionManager,
                                                        EventBus eventBus) {
    this.paymentTransactionManager = paymentTransactionManager;
    eventBus.register(this);
  }

  @Override
  @Subscribe
  public void aggregate(Transaction transaction) {
    paymentTransactionManager.upsert(transaction);
  }


}
