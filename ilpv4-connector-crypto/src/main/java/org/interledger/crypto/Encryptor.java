package org.interledger.crypto;

/**
 * Encrypt plaintext values using keys store in arbitrary KMS back-ends.
 */
public interface Encryptor {

  /**
   * Encrypt {@code secret} using this manager and its underlying secret store.
   *
   * @param keyMetadata         The {@link KeyMetadata} for this operation.
   * @param encryptionAlgorithm The {@link EncryptionAlgorithm} for this operation.
   * @param plainText           A byte array containing the plaintext value to encrypt.
   *
   * @return A {@link EncryptedSecret} containing everything needed to store the supplied {@code secret} securely in an
   * encrypted form.
   */
  EncryptedSecret encrypt(
    KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] plainText
  );
}
