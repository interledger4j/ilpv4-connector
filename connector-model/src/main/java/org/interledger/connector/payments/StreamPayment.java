package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * Payment representing the aggregation of STREAM packets that have been sent or received as part of a STREAM
 * payment.
 */
@JsonSerialize(as = ImmutableStreamPayment.class)
@JsonDeserialize(as = ImmutableStreamPayment.class)
public interface StreamPayment {

  static ImmutableStreamPayment.Builder builder() {
    return ImmutableStreamPayment.builder();
  }

  /**
   * Unique id for stream payment. Locally unique by accountId.
   *
   * @return
   */
  @Value.Default
  default String streamPaymentId() {
    return Hashing.sha256()
      .hashString(destinationAddress().getValue(), StandardCharsets.US_ASCII)
      .toString();
  }

  /**
   * AccountId that this stream payment is attached to.
   *
   * @return
   */
  AccountId accountId();

  /**
   * Source address that initiated the payment. Optional because in the case of payments received, the source address
   * will not be known if/until the sender sends a ConnectionNewAddress frame (which is not guaranteed).
   *
   * @return
   */
  Optional<InterledgerAddress> sourceAddress();

  /**
   * Destination address where the payment was sent.
   *
   * @return
   */
  InterledgerAddress destinationAddress();

  /**
   * Amount (in assetScale) of the accountId's account settings at the time the stream payment was created.
   * For payment's received, this will match {@link #deliveredAmount()}.
   *
   * @return
   */
  BigInteger amount();

  /**
   * Expected final amount (in assetScale of the accountId) of the payment.
   * This value will only be populated for payments initiated locally by the connector, or for payments fulfilled
   * locally by the connector.
   *
   * @return
   */
  Optional<BigInteger> expectedAmount();

  /**
   * Number of ILP packets that were aggregated into this stream payment.
   *
   * @return
   */
  int packetCount();

  /**
   * Asset code of the accountId's account settings at the time the stream payment was created.
   *
   * @return
   */
  String assetCode();

  /**
   * Asset scale of the accountId's account settings at the time the stream payment was created.
   *
   * @return
   */
  short assetScale();

  /**
   * Status of the stream payment.
   *
   * @return
   */
  StreamPaymentStatus status();

  /**
   * Type of stream payment (e.g. payment sent vs payment received)
   *
   * @return
   */
  StreamPaymentType type();

  /**
   * Amount delivered to {@link #destinationAddress()} in destination asset code/scale (if known).
   * This value will only be populated for payments initiated locally by the connector, or for payments fulfilled
   * locally by the connector. Otherwise value will be 0.
   *
   * @return
   */
  UnsignedLong deliveredAmount();

  /**
   * Amount delivered in receiver's asset code/scale.
   * This value will only be populated for payments initiated locally by the connector, or for payments fulfilled
   * locally by the connector AND the destination sent a {@code CONNECTION_ASSET_DETAILS} frame.
   *
   * @return
   */
  Optional<String> deliveredAssetCode();

  /**
   * Amount delivered in receiver's asset code/scale.
   * This value will only be populated for payments initiated locally by the connector, or for payments fulfilled
   * locally by the connector AND the destination sent a {@code CONNECTION_ASSET_DETAILS} frame.
   *
   * @return
   */
  Optional<Short> deliveredAssetScale();

  /**
   * When first packet on stream payment was received and aggregated into this payment
   *
   * @return
   */
  Instant createdAt();

  /**
   * Last time stream payment was modified, typically as a result of packets being aggregated or a status change.
   *
   * @return
   */
  Instant modifiedAt();

  @Value.Immutable
  abstract class AbstractStreamPayment implements StreamPayment {
    @Value.Check
    public AbstractStreamPayment validate() {
      if (type().getAdjustmentType().equals(StreamPaymentType.BalanceAdjustmentType.CREDIT)) {
        Preconditions.checkArgument(amount().compareTo(BigInteger.ZERO) >= 0, "cannot be negative for credit type");
      } else {
        Preconditions.checkArgument(amount().compareTo(BigInteger.ZERO) <= 0, "cannot be positive for debit type");
      }
      return this;
    }
  }

}
