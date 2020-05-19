package org.interledger.connector.opa.model;

import org.interledger.connector.opa.model.PaymentId;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.time.Instant;

/**
 * A user-facing Payment for transaction history purposes.
 */
@Value.Immutable
public interface Payment {

  static ImmutablePayment.Builder builder() {
    return ImmutablePayment.builder();
  }

  /**
   * Unique id for stream payment. Locally unique by accountId.
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
  Instant createdAt();

  /**
   * Last time stream payment was modified, typically as a result of packets being aggregated or a status change.
   *
   * @return An {@link Instant}.
   */
  Instant modifiedAt();

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
  BigInteger amount();

  /**
   * The asset code and scale of this payment.
   *
   * @return A {@link Denomination}.
   */
  Denomination denomination();

  /**
   * Type of stream payment (e.g. payment sent vs payment received)
   *
   * @return A {@link PaymentType}.
   */
  PaymentType type();

}
