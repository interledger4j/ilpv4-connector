package org.interledger.connector.events;

import org.interledger.connector.payments.ImmutableStreamPaymentClosedEvent;
import org.interledger.connector.payments.ImmutableStreamPaymentClosedEvent.Builder;
import org.interledger.connector.payments.StreamPayment;

import org.immutables.value.Value;

/**
 * An event emitted whenever a {@link StreamPayment} is closed.
 */
@Value.Immutable
public interface StreamPaymentClosedEvent extends ConnectorEvent {

  static Builder builder() {
    return ImmutableStreamPaymentClosedEvent.builder();
  }

  StreamPayment streamPayment();
}
