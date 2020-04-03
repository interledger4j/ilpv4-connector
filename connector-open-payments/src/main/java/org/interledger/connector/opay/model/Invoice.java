package org.interledger.connector.opay.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.Denomination;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.UUID;

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
   * The invoices URL of the Open Payments server.
   *
   * Used to generate a name for this invoice, as specified in the Open Payments specification.
   *
   * @see "https://docs.openpayments.dev/invoices#create".
   * @return The {@link HttpUrl} invoices endpoint.
   */
  @JsonIgnore
  HttpUrl openPaymentsInvoicesUrl();

  /**
   * A unique identifier for this {@link Invoice}.
   *
   * Defaults to a random UUID.
   *
   * @return The {@link UUID} of this {@link Invoice}.
   */
  @Value.Default
  default UUID invoiceId() {
    return UUID.randomUUID();
  }

  /**
   * The name of this {@link Invoice}, as specified here: https://docs.openpayments.dev/invoices#create.
   *
   * This will be the invoices URL of the Open Payments server followed by the invoice Id.
   *
   * @return The name of this Invoice.
   */
  @Value.Derived
  default String name() {
    return openPaymentsInvoicesUrl().newBuilder()
      .addPathSegment(invoiceId().toString())
      .build()
      .toString();
  }

  /**
   * Currency code or other asset identifier that this invoice is denominated in.
   * For example, `USD`, `EUR`, or `BTC`.
   *
   * @return A {@link String} containing the asset code.
   */
  String assetCode();

  /**
   * The asset scale as defined in {@link Denomination#assetScale()} that this invoice is denominated in.
   *
   * @return A short representing the asset scale.
   */
  short assetScale();

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
  UnsignedLong received();

  /**
   * The {@link PaymentPointer} of the receiver of the funds from this invoice.
   *
   * @return A {@link PaymentPointer} representing the receiver of this invoice.
   */
  PaymentPointer subject();

  /**
   * Invoices can no longer be paid after this time.
   *
   * @return The {@link Instant} at which this invoice is no longer valid.
   */
  Instant expiresAt();

  /**
   * The human readable description of this invoice.
   *
   * This is not required, but can give valuable information about the reason for this invoice.
   *
   * @return A {@link String} of the description of this invoice.
   */
  @Value.Default
  default String description() {
    return "";
  }

  /**
   * The ILP accountId of the subject of this invoice.
   *
   * @return The {@link AccountId} of the subject of this invoice.
   */
  @JsonIgnore
  AccountId accountId();

  /**
   * @return The {@link Instant} that this invoice was created.
   */
  @JsonIgnore
  default Instant createdAt() {
    return Instant.now();
  }

  /**
   * @return The {@link Instant} that this invoice was updated.
   */
  @JsonIgnore
  default Instant updatedAt() {
    return Instant.now();
  }

  /**
   * Represents the {@link Instant} in time when this invoice was finalized.
   *
   * An invoice is finalized when it is paid in full.
   *
   * @return The {@link Instant} when the invoice was finalized.
   */
  @JsonIgnore
  Instant finalizedAt();
}
