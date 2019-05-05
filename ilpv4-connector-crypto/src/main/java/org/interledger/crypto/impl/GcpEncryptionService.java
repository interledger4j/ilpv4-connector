package org.interledger.crypto.impl;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.Encryptor;
import org.interledger.crypto.KeyMetadata;

import java.io.IOException;
import java.util.Objects;

/**
 * An {@link Encryptor} and {@link Decryptor} that uses Google KMS to store private keys and otherwise perform sensitive
 * operations.
 */
public class GcpEncryptionService implements EncryptionService {

  private final String projectId;
  private final String locationId;

  private final KeyManagementServiceClient client;

  /**
   * Required-args constructor, unique to the GCP K
   *
   * @param projectId  The unique identifier of the GCP Project this keystore operates in.
   * @param locationId The canonical id for the locationId of the underlying keyring. For example: `us-east1`.
   */
  public GcpEncryptionService(final String projectId, final String locationId) {
    this.projectId = Objects.requireNonNull(projectId);
    this.locationId = Objects.requireNonNull(locationId);
    try {
      this.client = KeyManagementServiceClient.create();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public EncryptedSecret encrypt(
    final KeyMetadata keyMetadata,
    final EncryptionAlgorithm encryptionAlgorithm,
    final byte[] plainText
  ) {

    Objects.requireNonNull(keyMetadata);
    Objects.requireNonNull(encryptionAlgorithm);
    Objects.requireNonNull(plainText);

    // Example `keyName`: "projects/PROJECT_ID/locations/global/keyRings/RING_ID/cryptoKeys/KEY_ID/cryptoKeyVersions/1"
    final String keyName = CryptoKeyName.format(
      projectId, locationId, keyMetadata.keyringIdentifier(), keyMetadata.keyIdentifier()
    );

    // Encrypt the plaintext with Cloud KMS.
    EncryptResponse response = client.encrypt(keyName, ByteString.copyFrom(plainText));

    // Extract the ciphertext fromEncodedValue the response.
    return EncryptedSecret.builder()
      .keyMetadata(keyMetadata)
      .cipherMessage(response.getCiphertext().toByteArray())
      .build();
  }

  /**
   * Encrypt {@code secret} using this manager and its underlying secret store.
   *
   * @param keyMetadata         The {@link KeyMetadata} for this operation.
   * @param encryptionAlgorithm The {@link EncryptionAlgorithm} for this operation.
   * @param cipherMessage       A byte array containing the encrypted value to decrypt.
   *
   * @return A {@link EncryptedSecret} containing everything needed to store the supplied {@code secret} securely in an
   * encrypted form.
   */
  @Override
  public byte[] decrypt(KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] cipherMessage) {
    Objects.requireNonNull(keyMetadata);
    Objects.requireNonNull(encryptionAlgorithm);
    Objects.requireNonNull(cipherMessage);

    // Example `keyName`: "projects/PROJECT_ID/locations/global/keyRings/RING_ID/cryptoKeys/KEY_ID/cryptoKeyVersions/1"
    final String keyName = CryptoKeyName.format(
      projectId, locationId,
      keyMetadata.keyringIdentifier(),
      keyMetadata.keyIdentifier()
    );

    // Decrypt the ciphertext with Cloud KMS.
    final DecryptResponse response = client.decrypt(keyName, ByteString.copyFrom(cipherMessage));

    // Extract the plaintext fromEncodedValue the response.
    return response.getPlaintext().toByteArray();
  }

}
