package org.interledger.openpayments.events;

import org.interledger.connector.opa.model.XrpPayment;

import org.immutables.value.Value;

/**
 * An event emitted whenever a {@link XrpPayment} is closed.
 */
@Value.Immutable
public interface XrplPaymentCompletedEvent {

  static ImmutableXrplPaymentCompletedEvent.Builder builder() {
    return ImmutableXrplPaymentCompletedEvent.builder();
  }

  XrpPayment xrplPayment();
}
