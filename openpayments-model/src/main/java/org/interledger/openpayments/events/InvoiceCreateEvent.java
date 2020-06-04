package org.interledger.openpayments.events;

import org.interledger.openpayments.Invoice;
import org.immutables.value.Value;

/**
 * Event that is emitted when a new invoice has been created in an open-payments-enabled server operating with this
 * Connector.
 */
@Value.Immutable
public interface InvoiceCreateEvent {

  static ImmutableInvoiceCreateEvent.Builder builder() {
    return ImmutableInvoiceCreateEvent.builder();
  }

  /**
   * Placehold until an Invoice arrives.
   *
   * @return An {@link Invoice}.
   */
  Invoice invoice();
}
