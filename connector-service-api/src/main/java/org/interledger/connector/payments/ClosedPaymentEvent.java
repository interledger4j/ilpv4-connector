package org.interledger.connector.payments;

import org.immutables.value.Value;

@Value.Immutable
public interface ClosedPaymentEvent {

  static ImmutableClosedPaymentEvent.Builder builder() {
    return ImmutableClosedPaymentEvent.builder();
  }


  StreamPayment payment();

}
