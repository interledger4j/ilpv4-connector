package org.interledger.openpayments;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;
import org.interledger.openpayments.problems.InvalidInvoiceIdProblem;

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
      } catch (Exception e) {
        throw new InvalidInvoiceIdProblem("InvoiceIds must match the UUID format.", this);
      }
    }
  }

  /**
   * A wrapper that defines a unique identifier for a Payment.
   */
  @Value.Immutable
  @Wrapped
  abstract class _PaymentId extends Wrapper<String> {

    @Override
    public String toString() {
      return this.value();
    }
  }

  @Value.Immutable
  @Wrapped
  abstract class _CorrelationId extends Wrapper<String> {

    @Override
    public String toString() {
      return this.value();
    }
  }

  /**
   * A wrapper that defines a unique identifier for a Mandate.
   */
  @Value.Immutable
  @Wrapped
  abstract class _MandateId extends Wrapper<String> {

    @Override
    public String toString() {
      return this.value();
    }
  }


  /**
   * A wrapper that defines a unique identifier for a Charge.
   */
  @Value.Immutable
  @Wrapped
  abstract class _ChargeId extends Wrapper<String> {

    @Override
    public String toString() {
      return this.value();
    }
  }

}
