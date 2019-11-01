package org.interledger.crypto;

/**
 * Decrypt ciphertext values using keys stored in arbitrary KMS back-ends.
 */
public interface Decryptor {

  /**
   * Load an actual secret using the supplied information.
   *
   * @param encryptedSecret The {@link EncryptedSecret} to use to load an actual secret.
   *
   * @return A byte array containing the unencrypted secret value, in binary form.
   */
  default byte[] decrypt(final EncryptedSecret encryptedSecret) {
    return decrypt(
        encryptedSecret.keyMetadata(),
        encryptedSecret.encryptionAlgorithm(),
        encryptedSecret.cipherMessage()
    );
  }

  /**
   * Encrypt {@code secret} using this manager and its underlying secret store.
   *
   * @param keyMetadata         The {@link KeyMetadata} for this operation.
   * @param encryptionAlgorithm The {@link EncryptionAlgorithm} for this operation.
   * @param cipherMessage       A byte array containing the encrypted value to decrypt.
   *
   * @return A byte-array containing the original plainText that was encrypted.
   */
  byte[] decrypt(
      KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] cipherMessage
  );
}
