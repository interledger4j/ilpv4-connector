package org.interledger.openpayments.events;

import org.interledger.openpayments.xrpl.XrplTransaction;

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
   * @return An {@link XrplTransaction}.
   */
  XrplTransaction payment();
}
