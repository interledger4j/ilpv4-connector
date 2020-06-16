package org.interledger.openpayments;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;
import org.interledger.openpayments.problems.InvalidInvoiceIdProblem;
import org.interledger.openpayments.problems.InvalidPayIdAccountIdProblem;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.io.Serializable;
import java.util.Locale;
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

  @Value.Immutable
  @Wrapped
  @JsonSerialize(as = PayIdAccountId.class)
  @JsonDeserialize(as = PayIdAccountId.class)
  abstract class _PayIdAccountId extends Wrapper<String> implements Serializable {

    @Override
    public String toString() {
      return this.value();
    }

    @Value.Check
    public _PayIdAccountId enforceSize() {
      if (this.value().length() > 64) {
        throw new InvalidPayIdAccountIdProblem("PayIdAccountId must not be longer than 64 characters");
      } else {
        return this;
      }
    }

    /**
     * Ensures that an accountId only ever contains lower-cased US-ASCII letters (not upper-cased).
     *
     * @return A normalized {@link PayIdAccountId}.
     *
     * @see "https://github.com/interledger4j/ilpv4-connector/issues/623"
     */
    @Value.Check
    public _PayIdAccountId normalizeToLowercase() {
      if (this.value().chars().anyMatch(Character::isUpperCase)) {
        return PayIdAccountId.of(this.value().toLowerCase(Locale.ENGLISH));
      } else {
        return this;
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
