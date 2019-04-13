package org.interledger.secrets.gcp;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * Loads secrets from Google KMS.
 */
public class GcpSecretLoader {

  private final String projectId;
  private final String locationId;

  private final KeyManagementServiceClient client;

  /**
   * @param projectId  The unique identifier for this project.
   * @param locationId The canonical id for this locationId. For example: `us-east1`.
   */
  public GcpSecretLoader(final String projectId, final String locationId) {
    this.projectId = Objects.requireNonNull(projectId);
    this.locationId = Objects.requireNonNull(locationId);
    try {
      this.client = KeyManagementServiceClient.create();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * For any properties that begin with the prefix "gcpkms", attempt to decrypt them using the specified key meta-data.
   */
  public ByteString loadSecretFromGoogleKms(final GcpEncodedSecret gcpEncodedSecret) {
    Objects.requireNonNull(gcpEncodedSecret);

    // Make the RPC call
    final String resourceName = CryptoKeyName.format(projectId, locationId, gcpEncodedSecret.keyringIdentifier(),
      gcpEncodedSecret.keyIdentifier());

    // Decrypt the ciphertext with Cloud KMS.
    final DecryptResponse response =
      client.decrypt(resourceName, ByteString.copyFrom(
        Base64.getDecoder().decode(gcpEncodedSecret.cipherText()))
      );

    // Extract the plaintext from the response.
    return response.getPlaintext();
  }

}
