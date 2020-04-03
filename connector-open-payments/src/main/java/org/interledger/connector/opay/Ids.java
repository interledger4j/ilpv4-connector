package org.interledger.connector.opay;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.io.Serializable;
import java.util.UUID;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * A wrapper that defines a unique identifier for an invoice.
   */
  @Value.Immutable(intern = true)
  @Wrapped
  @JsonSerialize(as = InvoiceId.class)
  @JsonDeserialize(as = InvoiceId.class)
  static abstract class _InvoiceId extends Wrapper<UUID> implements Serializable {

    @Override
    public String toString() {
      return this.value().toString();
    }

  }

}
