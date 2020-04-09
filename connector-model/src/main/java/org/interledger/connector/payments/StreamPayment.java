package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

/**
 * Payment representing the aggregation of STREAM packets that have been sent or received as part of a STREAM
 * payment.
 */
@Value.Immutable
public interface StreamPayment {

  static ImmutableStreamPayment.Builder builder() {
    return ImmutableStreamPayment.builder();
  }

  /**
   * Unique id for stream payment. Locally unique by accountId.
   * @return
   */
  String streamPaymentId();

  /**
   * AccountId that this stream payment is attached to.
   * @return
   */
  AccountId accountId();

  /**
   * Source address that initiated the payment. Optional because in the case of payments received, the source address
   * will not be known if/until the sender sends a ConnectionNewAddress frame (which is not guaranteed).
   * @return
   */
  Optional<InterledgerAddress> sourceAddress();

  /**
   * Destination address where the payment was sent.
   * @return
   */
  InterledgerAddress destinationAddress();

  /**
   * Amount (in assetScale) of the accountId's account settings at the time the stream payment was created.
   * @return
   */
  BigInteger amount();

  /**
   * Number of ILP packets that were aggregated into this stream payment.
   * @return
   */
  int packetCount();

  /**
   * Asset code of the accountId's account settings at the time the stream payment was created.
   * @return
   */
  String assetCode();


  /**
   * Asset scale of the accountId's account settings at the time the stream payment was created.
   * @return
   */
  short assetScale();

  /**
   * Status of the stream payment.
   * @return
   */
  StreamPaymentStatus status();

  /**
   * Type of stream payment (e.g. payment sent vs payment received)
   * @return
   */
  StreamPaymentType type();

  /**
   * When first packet on stream payment was received and aggregated into this payment
   * @return
   */
  Instant createdAt();

  /**
   * Last time stream payment was modified, typically as a result of packets being aggregated or a status change.
   * @return
   */
  Instant modifiedAt();

}
