package org.interledger.openpayments.events;

import org.interledger.openpayments.CorrelationId;

public interface PaymentCompletedEvent {

  CorrelationId paymentCorrelationId();

}
