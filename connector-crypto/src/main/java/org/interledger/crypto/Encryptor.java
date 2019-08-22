package org.interledger.crypto;

/**
 * Encrypt plaintext values using keys store in arbitrary KMS back-ends.
 */
public interface Encryptor {

  /**
   * Encrypt {@code plainText} using the underlying key-store of this implementation.
   *
   * @param keyMetadata         The {@link KeyMetadata} for this operation.
   * @param encryptionAlgorithm The {@link EncryptionAlgorithm} for this operation.
   * @param plainText           A byte-array containing the plaintext value to encrypt.
   *
   * @return An {@link EncryptedSecret} containing everything needed to store the supplied {@code plainText} securely in
   * an encrypted form.
   */
  EncryptedSecret encrypt(
    KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] plainText
  );
}
