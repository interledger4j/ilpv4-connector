package org.interledger.openpayments.events;

import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.xrpl.XrplTransaction;

import org.immutables.value.Value;

/**
 * Event that is emitted when an open payment payment is declined.
 */
@Value.Immutable
public interface PaymentDeclinedEvent {

  static ImmutablePaymentDeclinedEvent.Builder builder() {
    return ImmutablePaymentDeclinedEvent.builder();
  }

  /**
   * The correlationId payment that was declined.
   *
   * @return An {@link XrplTransaction}.
   */
  CorrelationId paymentCorrelationId();

}
