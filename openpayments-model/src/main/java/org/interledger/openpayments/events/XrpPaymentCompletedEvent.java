package org.interledger.openpayments.events;

import org.interledger.connector.opa.model.XrpPayment;

import org.immutables.value.Value;

/**
 * Event that is emitted when an XRP payment has completed.
 */
@Value.Immutable
public interface XrpPaymentCompletedEvent {

  static ImmutableXrpPaymentCompletedEvent.Builder builder() {
    return ImmutableXrpPaymentCompletedEvent.builder();
  }

  /**
   * The XRP payment that was completed on the XRP Ledger.
   *
   * @return An {@link XrpPayment}.
   */
  XrplTransaction payment();
}
