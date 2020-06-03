package org.interledger.connector.opa.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Represents an amount payable that can be presented to a third party and/or a Mandate to pay.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableInvoice.class)
@JsonDeserialize(as = ImmutableInvoice.class)
public interface Invoice {

  static ImmutableInvoice.Builder builder() {
    return ImmutableInvoice.builder();
  }

  /**
   * The true unique identifier of this {@link Invoice}. If sender and receiver on the same OPS, or multiple
   * senders pay the same invoice from the same OPS, there could be multiple copies of an {@link Invoice} with the same
   * {@link Invoice#id()}.
   *
   * @return A unique identifier for this {@link Invoice}.
   */
  @JsonIgnore
  @Value.Default
  default Long primaryKey() {
    return 0L;
  };

  /**
   * The location of this {@link Invoice}, specified by an HTTP URL.
   *
   * Optional for invoice creation requests, but MUST be populated once created.
   *
   * @return The unique {@link HttpUrl} of this {@link Invoice}.
   */
  @JsonProperty("name")
  Optional<HttpUrl> invoiceUrl();

  /**
   * The identifier of this {@link Invoice}. If sender and receiver on the same OPS, or multiple
   * senders pay the same invoice from the same OPS, there could be multiple copies of an {@link Invoice} with the same
   * {@link Invoice#id()}.
   *
   * Defaults to a random UUID.
   *
   * @return The {@link UUID} of this {@link Invoice}.
   */
  @Value.Default
  default InvoiceId id() {
    return InvoiceId.of(UUID.randomUUID().toString());
  }

  /**
   * Some form of this {@link Invoice}'s {@link Invoice#id()} such that it can be attached to a payment to later
   * correlate the payment to this {@link Invoice}.
   *
   * Defaults to a SHA-256 hash of {@link Invoice#id()}.
   *
   * @return A {@link CorrelationId} containing an identifier which can be included in a payment to correlate it to this
   *  {@link Invoice}.
   */
  @Value.Default
  default CorrelationId correlationId() {
    HashCode hashCode = Hashing.sha256()
      .hashString(id().value(), StandardCharsets.UTF_8);
    return CorrelationId.of(hashCode.toString());
  };

  /**
   * Currency code or other asset identifier that this invoice is denominated in.
   * For example, `USD`, `EUR`, or `BTC`.
   *
   * @return A {@link String} containing the asset code.
   */
  String assetCode();

  /**
   * <p>An asset scale is the difference, in orders of magnitude, between an asset's `standard unit` and a
   * corresponding `fractional unit`.</p>
   *
   * <p>A standard unit represents the typical unit of account for a particular asset. For example 1 USD in the case of
   * U.S. dollars, or 1 BTC in the case of Bitcoin (Note that peers are free to define this value in any way, but
   * participants in an Interledger accounting relationship must be sure to use the same value. Thus, it is suggested to
   * use typical values when possible).</p>
   *
   * <p>A fractional unit represents some unit smaller than its corresponding standard unit, but with greater
   * precision. Examples of fractional monetary units include one cent ($0.01 USD), or 1 satoshi (0.00000001 BTC).</p>
   *
   * <p>Because Interledger amounts are integers, but most currencies are typically represented as fractional units
   * (e.g. cents), this property defines how many Interledger units make up one standard unit of the asset code
   * specified above.</p>
   *
   * <p>More formally, the asset scale is a non-negative integer (0, 1, 2, …) such that one standard unit equals
   * 10^(-scale) of a corresponding fractional unit. If the fractional unit equals the standard unit, then the asset
   * scale is 0.</p>
   *
   * <p>For example, one "cent" represents an asset scale of 2 in the case of USD; 1 satoshi represents an asset scale
   * of 8 in the case of Bitcoin; and 1 drop represents an asset scale of 6 in XRP.</p>
   *
   * @return A {@link Short} representing the asset scale.
   */
  short assetScale();

  /**
   * The account ID of the creator of this invoice.
   *
   * For ILP invoices, this will be the {@link String} form of an AccountId. For other payment networks,
   * this may be the account ID of a wallet holder.
   *
   * @return A {@link String} representing the account ID of the user who created this invoice.
   */
  @Value.Default
  default AccountId accountId() {
    // FIXME: We don't have to default this value once we derive creation URL from the payment pointer
    try {
      PaymentPointer paymentPointerSubject = PaymentPointer.of(subject());
      return AccountId.of(paymentPointerSubject.path().substring(paymentPointerSubject.path().lastIndexOf("/") + 1));
    } catch (IllegalArgumentException e) {
      // subject is a PayID
      String payIdString = subject();
      if (!payIdString.startsWith("payid:")) {
        payIdString = "payid:" + payIdString;
      }
      PayId payId = PayId.of(payIdString);
      return AccountId.of(payId.account());
    }
  };

  /**
   * The amount that should be paid to this invoice, denominated in {@code assetCode()} and {@code assetScale()}.
   *
   * An invoice has not been fully paid until {@code received()} equals {@code amount()}.
   *
   * @return An {@link UnsignedLong} representing the amount that should be paid to this invoice.
   */
  UnsignedLong amount();

  /**
   * The amount that has been paid to this invoice, denominated in {@code assetCode()} and {@code assetScale()}.
   *
   * An invoice has not been fully paid until {@code received()} equals {@code amount()}.
   *
   * @return An {@link UnsignedLong} representing the amount that has been paid to this invoice.
   */
  @Value.Default
  default UnsignedLong received() {
    return UnsignedLong.ZERO;
  };

  /**
   * The identifier of the receiver of the funds from this invoice. For ILP payments this will be a Payment Pointer.
   * For XRP payments, this will likely be an XRP address or PayID.
   *
   * @return A {@link String} representing the identifier of the receiver of this invoice.
   */
  String subject();

  /**
   * Invoices can no longer be paid after this time.
   *
   * @return The {@link Instant} at which this invoice is no longer valid.
   */
  @Value.Default
  default Instant expiresAt() {
   return Instant.now().plus(Duration.ofDays(1));
  };

  /**
   * The human readable description of this invoice.
   *
   * This is not required, but can give valuable information about the reason for this invoice.
   *
   * @return A {@link String} of the description of this invoice.
   */
  @Nullable
  String description();

  /**
   * @return The {@link Instant} that this invoice was created.
   */
  @Value.Default
  default Instant createdAt() {
    return Instant.now();
  }

  /**
   * @return The {@link Instant} that this invoice was updated.
   */
  @Value.Default
  default Instant updatedAt() {
    return Instant.now();
  }

  /**
   * Simple check to see if this {@link Invoice} has been paid.
   *
   * @return true if amount received is greater than or equal to the amount of the {@link Invoice}, otherwise false.
   */
  @JsonIgnore
  @Value.Derived
  default boolean isPaid() {
    return amount().longValue() <= received().longValue();
  }

  /**
   * Represents the {@link Instant} in time when this invoice was finalized.
   *
   * An invoice is finalized when it is paid in full.
   *
   * @return The {@link Instant} when the invoice was finalized.
   */
  @JsonIgnore
  @Nullable
  Instant finalizedAt();

}
