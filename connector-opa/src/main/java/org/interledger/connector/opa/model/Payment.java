package org.interledger.connector.opa.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.PaymentId;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

/**
 * A user-facing Payment for transaction history purposes.
 */
@Value.Immutable
public interface Payment {

  static ImmutablePayment.Builder builder() {
    return ImmutablePayment.builder();
  }

  @Deprecated
  AccountId accountId();

  Long invoicePrimaryKey();

  /**
   * Correlation id provided by external clients to correlate this stream payment to their systems. Not required and
   * does not need to be unique.
   *
   * @return
   */
  String correlationId();

  /**
   * Unique id for a payment. Locally unique by invoiceId.
   *
   * @return A {@link String} containing the unique identifier of this STREAM payment.
   */
  PaymentId paymentId();

  /**
   * Last time stream payment was modified, typically as a result of packets being aggregated or a status change.
   *
   * /** When first packet on stream payment was received and aggregated into this payment
   *
   * @return An {@link Instant}.
   */
  @Value.Default
  default Instant createdAt() {
    return Instant.now();
  };

  /**
   * Last time stream payment was modified, typically as a result of packets being aggregated or a status change.
   *
   * @return An {@link Instant}.
   */
  @Value.Default
  default Instant modifiedAt() {
    return Instant.now();
  };

  /**
   * Source address that initiated the payment. Optional because in the case of payments received, the source address
   * will not be known if/until the sender sends a ConnectionNewAddress frame (which is not guaranteed).
   *
   * @return A {@link String}.
   */
  String sourceAddress();

  /**
   * Destination address where the payment was sent.
   *
   * @return An {@link String}.
   */
  String destinationAddress();

  /**
   * Amount (in assetScale) of the accountId's account settings at the time the stream payment was created.
   *
   * @return A {@link BigInteger}.
   */
  UnsignedLong amount();

  /**
   * The asset code and scale of this payment.
   *
   * @return A {@link Denomination}.
   */
  Denomination denomination();

}
