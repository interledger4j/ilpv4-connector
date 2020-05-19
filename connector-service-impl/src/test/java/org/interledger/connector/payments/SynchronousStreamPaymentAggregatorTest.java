package org.interledger.connector.payments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.events.FulfillmentGeneratedEvent;

import org.junit.Test;

public class SynchronousStreamPaymentAggregatorTest {

  @Test
  public void aggregateDelegatesToTrxManager() {
    StreamPaymentManager mockTrxManager = mock(StreamPaymentManager.class);
    FulfillmentGeneratedEventConverter converter = mock(FulfillmentGeneratedEventConverter.class);
    FulfillmentGeneratedEvent mockEvent = mock(FulfillmentGeneratedEvent.class);
    StreamPayment mockTrx = mock(StreamPayment.class);
    when(converter.convert(mockEvent)).thenReturn(mockTrx);

    new SynchronousFulfillmentGeneratedEventAggregator(mockTrxManager, converter).aggregate(mockEvent);
    verify(mockTrxManager).merge(mockTrx);
  }

}
