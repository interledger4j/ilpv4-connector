package org.interledger.connector.opa.ilp;

import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.StreamException;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

public class OpaStreamConnectionGenerator implements StreamConnectionGenerator {
  private static final Charset US_ASCII = StandardCharsets.US_ASCII;

  private final byte[] streamServerSecretGenerator;

  /**
   * No-args Constructor.
   */
  public OpaStreamConnectionGenerator() {
    // Note that by default, we are using the same magic bytes as the Javascript implementation but this is not
    // strictly necessary. These magic bytes need to be the same for the server that creates the STREAM details for a
    // given packet and for the server that fulfills those packets, but in the vast majority of cases those two servers
    // will be running the same STREAM implementation so it doesn't matter what this string is. However, for more
    // control, see the required-args Constructor.
    this("ilp_stream_shared_secret");
  }

  /**
   * Required-args constructor.
   *
   * @param streamServerSecretGenerator A set of magic bytes that act as a secret-generator seed for generating SPSP
   *                                    shared secrets. These magic bytes need to be the same for the server that
   *                                    creates the STREAM details for a given packet and for the server that fulfills
   *                                    those packets, but in the vast majority of cases those two servers will be
   *                                    running the same STREAM implementation so it doesn't matter what this string
   *                                    is.
   */
  public OpaStreamConnectionGenerator(final String streamServerSecretGenerator) {
    this.streamServerSecretGenerator = Objects.requireNonNull(streamServerSecretGenerator)
      .getBytes(StandardCharsets.US_ASCII);
  }

  /**
   * Note that this is simply a copy of {@link SpspStreamConnectionGenerator}'s method.  However,
   * Open Payments addresses should have the encoded {@link InvoiceId} as part of
   * the connection tag.  Callers must manually add the invoice id to the connection tag.
   */
  @Override
  public StreamConnectionDetails generateConnectionDetails(ServerSecretSupplier serverSecretSupplier, InterledgerAddress receiverAddress) throws StreamException {
    Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    Objects.requireNonNull(receiverAddress, "receiverAddress must not be null");
    Preconditions.checkArgument(serverSecretSupplier.get().length >= 32, "Server secret must be 32 bytes");

    final byte[] token = Random.randBytes(18);
    final String tokenBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    final InterledgerAddress destinationAddress = receiverAddress.with(tokenBase64);

    // Note the shared-secret is generated from the token's base64-encoded String bytes rather than from the
    // _actual_ Base64-unencoded bytes. E.g., "foo".getBytes() is not the same as Base64.getDecoder().decode("foo")
    final byte[] sharedSecret = Hashing
      .hmacSha256(secretGenerator(serverSecretSupplier))
      .hashBytes(tokenBase64.getBytes(StandardCharsets.US_ASCII))
      .asBytes();

    return StreamConnectionDetails.builder()
      .destinationAddress(destinationAddress)
      .sharedSecret(SharedSecret.of(sharedSecret))
      .build();
  }

  @Override
  public SharedSecret deriveSecretFromAddress(ServerSecretSupplier serverSecretSupplier, InterledgerAddress receiverAddress) throws StreamException {
    Objects.requireNonNull(receiverAddress);

    final String receiverAddressAsString = receiverAddress.getValue();
    // For Javascript compatibility, the `localpart` is not treated as a base64-encoded string of bytes, but is instead
    // treated simply as US-ASCII bytes.
    final String localPart = receiverAddressAsString.substring(
      receiverAddressAsString.lastIndexOf(".") + 1,
      receiverAddressAsString.lastIndexOf("~") // Strip the invoiceId connection tag
    );

    final byte[] sharedSecret = Hashing
      .hmacSha256(secretGenerator(serverSecretSupplier))
      .hashBytes(localPart.getBytes(StandardCharsets.US_ASCII))
      .asBytes();
    return SharedSecret.of(sharedSecret);
  }

  /**
   * Helper method to compute HmacSha256 on {@link #streamServerSecretGenerator}.
   *
   * @param serverSecretSupplier A {@link Supplier} for this node's main secret, which is the root seed for all derived
   *                             secrets provided by this node.
   *
   * @return A secret derived from a primary secret.
   */
  private byte[] secretGenerator(final ServerSecretSupplier serverSecretSupplier) {
    Objects.requireNonNull(serverSecretSupplier);
    return Hashing.hmacSha256(serverSecretSupplier.get()).hashBytes(this.streamServerSecretGenerator).asBytes();
  }
}
