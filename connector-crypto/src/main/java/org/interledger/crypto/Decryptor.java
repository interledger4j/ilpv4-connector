package org.interledger.crypto;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Decrypt ciphertext values using keys stored in arbitrary KMS back-ends.
 */
public interface Decryptor {

  /**
   * Load an actual secret using the supplied information.
   *
   * @param encryptedSecret The {@link EncryptedSecret} to use to load an actual secret.
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
   * @return A byte-array containing the original plainText that was encrypted.
   */
  byte[] decrypt(
      KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] cipherMessage
  );

  /**
   * Wrapper for accessing a decrypted secret that afterward cleans up the decrypted bytes in memory
   * for better security.
   * <p>
   *   For example, the following code:
   *   <pre>
   *     byte[] decrypted = decryptor.decrypt(encryptedSecret);
   *     boolean valid = validateSecret(decrypted);
   *   </pre>
   *   can instead be written as:
   *   <pre>
   *     boolean valid = decryptor.withDecrypted(encryptedSecret,
   *                                            (decrypted) -> validateSecret(decrypted));
   *   </pre>
   * </p>
   *
   * @param encryptedSecret secret to decrupt
   * @param callable lambda to execute with access to the decrypted secret
   * @param <T> return type
   * @return result of call the callable with the decrypted secret
   */
  default <T> T withDecrypted(EncryptedSecret encryptedSecret, Function<byte[], T> callable) {
    // decrypt
    byte[] decrypted = decrypt(encryptedSecret);
    try {
      return callable.apply(decrypted);
    } finally {
      Arrays.fill(decrypted, (byte) 0);
    }
  }

  /**
   * Checks if the encrypted secret matches decrypts to an expected result. Equality check is done with a
   * constant-time algorithm way to prevent timing based attacks
   * {@see https://codahale.com/a-lesson-in-timing-attacks/}
   * @param encrypted encrypted value to check
   * @param expected expected decrypted bytes
   * @return true if encrypted decrypts to the expected byte array
   */
  default boolean isEqualDecrypted(EncryptedSecret encrypted, byte[] expected) {
    return withDecrypted(encrypted, decrypted -> ByteArrays.isEqualUsingConstantTime(decrypted, expected));
  }

}
