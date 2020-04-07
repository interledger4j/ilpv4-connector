package org.interledger.connector.opa.model;

import org.interledger.connector.accounts.AccountId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.time.Instant;
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
   * A unique identifier for this {@link Invoice}.
   *
   * Defaults to a random UUID.
   *
   * @return The {@link UUID} of this {@link Invoice}.
   */
  @Value.Default
  default InvoiceId id() {
    return InvoiceId.of(UUID.randomUUID());
  }

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
   * For ILP invoices, this will be the {@link String} form of an {@link AccountId}. For other payment networks,
   * this may be the account ID of a wallet holder.
   *
   * @return A {@link String} representing the account ID of the user who created this invoice.
   */
  String accountId();

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
   * The identifier of the receiver of the funds from this invoice. For ILP payments this will be a Payment Pointer.
   * For XRP payments, this will likely be an XRP address.
   *
   * @return A {@link String} representing the identifier of the receiver of this invoice.
   */
  String subject();

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
  @Nullable
  String description();

  /**
   * @return The {@link Instant} that this invoice was created.
   */
  @JsonIgnore
  @Value.Default
  default Instant createdAt() {
    return Instant.now();
  }

  /**
   * @return The {@link Instant} that this invoice was updated.
   */
  @JsonIgnore
  @Value.Default
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
  @Nullable
  Instant finalizedAt();
}
