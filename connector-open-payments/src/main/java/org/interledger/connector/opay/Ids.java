package org.interledger.connector.opay;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;

import org.immutables.value.Value;

import java.util.UUID;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public interface Ids {

  /**
   * A wrapper that defines a unique identifier for an invoice.
   */
  @Value.Immutable
  @Wrapped
  abstract class _InvoiceId extends Wrapper<UUID> {

    @Override
    public String toString() {
      return this.value().toString();
    }

  }

}
