package org.interledger.connector.transactions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

import org.junit.Test;

public class SynchronousPaymentTransactionAggregatorTest {

  @Test
  public void aggregateDelegatesToTrxManager() {
    PaymentTransactionManager mockTrxManager = mock(PaymentTransactionManager.class);
    FulfillmentGeneratedEventConverter converter = mock(FulfillmentGeneratedEventConverter.class);
    FulfillmentGeneratedEvent mockEvent = mock(FulfillmentGeneratedEvent.class);
    Transaction mockTrx = mock(Transaction.class);
    when(converter.convert(mockEvent)).thenReturn(mockTrx);

    new SynchronousFulfillmentGeneratedEventAggregator(mockTrxManager, converter).aggregate(mockEvent);
    verify(mockTrxManager).merge(mockTrx);
  }

}