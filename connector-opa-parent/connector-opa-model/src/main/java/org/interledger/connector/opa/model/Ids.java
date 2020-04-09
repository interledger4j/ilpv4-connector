package org.interledger.connector.opa.model;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;
import org.interledger.connector.opa.model.problems.InvalidInvoiceIdProblem;

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
  abstract class _InvoiceId extends Wrapper<String> {

    @Override
    public String toString() {
      return this.value();
    }

    @Value.Check
    public _InvoiceId followsUuidFormat() {
      try {
        UUID.fromString(this.value());
        return this;
      }
      catch (Exception e) {
        throw new InvalidInvoiceIdProblem("InvoiceIds must match the UUID format.", this);
      }
    }
  }

}
