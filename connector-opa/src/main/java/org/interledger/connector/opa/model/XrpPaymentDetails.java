package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Payment details necessary to pay an {@link Invoice} over XRP Ledger.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableXrpPaymentDetails.class)
@JsonDeserialize(as = ImmutableXrpPaymentDetails.class)
public interface  XrpPaymentDetails {

  static ImmutableXrpPaymentDetails.Builder builder() {
    return ImmutableXrpPaymentDetails.builder();
  }

  /**
   * The XRP address to pay for an {@link Invoice}. Can be either an X-Address or Classic Address.
   *
   * @return The public XRP address that should be paid to pay an {@link Invoice}.
   */
  String address();

  Optional<Integer> addressTag();

  /**
   * The 256-bit hash of an {@link Invoice}'s ID, which can be attached to an XRPL transaction in the InvoiceID field.
   *
   * @return The 256-bit hash of an {@link Invoice}'s ID.
   */
  String invoiceIdHash();

  Optional<String> instructions();

}
