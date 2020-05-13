package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

/**
 * Payment representing the aggregation of STREAM packets that have been sent or received as part of a STREAM payment.
 */
@Value.Immutable
public interface StreamPayment {

  static ImmutableStreamPayment.Builder builder() {
    return ImmutableStreamPayment.builder();
  }

  /**
   * Unique id for stream payment. Locally unique by accountId.
   *
   * @return A {@link String} containing the unique identifier of this STREAM payment.
   */
  String streamPaymentId();

  /**
   * AccountId that this stream payment is attached to.
   *
   * @return An {@link AccountId}.
   */
  AccountId accountId();

  /**
   * Source address that initiated the payment. Optional because in the case of payments received, the source address
   * will not be known if/until the sender sends a ConnectionNewAddress frame (which is not guaranteed).
   *
   * @return An optionally-present {@link InterledgerAddress}.
   */
  Optional<InterledgerAddress> sourceAddress();

  /**
   * Destination address where the payment was sent.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress destinationAddress();

  /**
   * Amount (in assetScale) of the accountId's account settings at the time the stream payment was created.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger amount();

  /**
   * Number of ILP packets that were aggregated into this stream payment.
   *
   * @return An int representing the number packets involved in this payment.
   */
  int packetCount();

  /**
   * Asset code of the accountId's account settings at the time the stream payment was created.
   *
   * @return A {@link String} containing the asset code.
   */
  String assetCode();


  /**
   * Asset scale of the accountId's account settings at the time the stream payment was created.
   *
   * @return A short representing the asset scale.
   */
  short assetScale();

  /**
   * Status of the stream payment.
   *
   * @return A {@link StreamPaymentStatus}.
   */
  StreamPaymentStatus status();

  /**
   * Type of stream payment (e.g. payment sent vs payment received)
   *
   * @return A {@link StreamPaymentType}.
   */
  StreamPaymentType type();

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

}
