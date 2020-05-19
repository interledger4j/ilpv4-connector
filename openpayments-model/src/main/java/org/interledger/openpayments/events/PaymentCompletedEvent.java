package org.interledger.openpayments.events;

import org.interledger.connector.opa.model.Payment;

import org.immutables.value.Value;

/**
 * Event that is emitted when a STREAM payment has completed.
 */
@Value.Immutable
public interface PaymentCompletedEvent {

  static ImmutablePaymentCompletedEvent.Builder builder() {
    return ImmutablePaymentCompletedEvent.builder();
  }

  /**
   * The Stream Payment that was updated by the underlying payment system.
   *
   * @return A {@link Payment}.
   */
  Payment payment();
}
