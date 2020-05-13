package org.interledger.connector.events;

import org.interledger.connector.opa.model.Invoice;

import org.immutables.value.Value;

/**
 * Event that is emitted when a new invoice has been created by an associated open-payments server.
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
