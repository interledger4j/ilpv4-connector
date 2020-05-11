package org.interledger.connector.opa.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableIlpPaymentDetails.class)
@JsonDeserialize(as = ImmutableIlpPaymentDetails.class)
public interface IlpPaymentDetails extends PaymentDetails {

  static ImmutableIlpPaymentDetails.Builder builder() {
    return ImmutableIlpPaymentDetails.builder();
  }

  static IlpPaymentDetails from(StreamConnectionDetails streamConnectionDetails) {
    return builder()
      .destinationAddress(streamConnectionDetails.destinationAddress())
      .sharedSecret(streamConnectionDetails.sharedSecret())
      .build();
  }

  /**
   * <p>The ultimate destination of an ILPv4 packet that is part of a STREAM.</p>
   *
   * <p>This address is generated such that the `shared_secret` can be re-derived from a Prepare packet's destination
   * and the same server secret. If the address is modified in any way, the server will not be able to re-derive the
   * secret and the packet will be rejected.
   *
   * @return An {@link InterledgerAddress} that can receive and fulfill ILPv4 packets.
   */
  @JsonProperty("destination_account")
  InterledgerAddress destinationAddress();

  /**
   * The shared secret to be used by a specific HTTP client in a STREAM. Should be shared only by the server that
   * generated and a specific HTTP client, and should therefore be different for each query response. Even though
   * clients SHOULD accept base64url encoded secrets, base64 encoded secrets are recommended.
   *
   * @return A {@link String}.
   */
  @JsonProperty("shared_secret")
  SharedSecret sharedSecret();
}
