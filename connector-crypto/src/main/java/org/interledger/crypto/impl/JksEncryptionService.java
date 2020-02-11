package org.interledger.crypto.impl;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionAlgorithm;
import org.interledger.crypto.EncryptionException;
import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.KeyStoreType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * An {@link EncryptionService} that uses a JavaKeystore for underlying key storage.
 */
public class JksEncryptionService implements EncryptionService {

  private static final String CIPHER_ALGO = "AES/GCM/NoPadding";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final SecureRandom secureRandom;
  private final SecretKey secretKey;

  /**
   * Required-args Constructor. This variation only supports a common password for all JKS files.
   */
  public JksEncryptionService(SecretKey secretKey) throws NoSuchAlgorithmException {
    this.secretKey = Objects.requireNonNull(secretKey);
    this.secureRandom = SecureRandom.getInstance("NativePRNG");
  }

  @Override
  public KeyStoreType keyStoreType() {
    return KeyStoreType.JKS;
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

    if (!secretKey.getAlgorithm().equalsIgnoreCase("AES")) {
      logger.error("Invalid key algorithm {}", secretKey.getAlgorithm());
      throw new EncryptionException("Invalid Key. Only AES keys are supported");
    }

    // Create an initialization vector. For GCM a 12 byte random byte-array is recommend by NIST
    // because it's faster and more secure. See here, page 8:
    // https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf
    final byte[] iv = new byte[12];
    secureRandom.nextBytes(iv);

    // See https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      // 128 is the recommended authentication tag length for GCM . More info can be found in pdf mentioned above
      final GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
      byte[] cipherText = cipher.doFinal(plainText);
      // Concatenate to a single message
      final ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
      byteBuffer.putInt(iv.length);
      byteBuffer.put(iv);
      byteBuffer.put(cipherText);
      final byte[] cipherMessage = byteBuffer.array();


      // It is best practice to try to wipe sensible data like a cryptographic key or IV from memory as fast as
      // possible. Since Java is a language with automatic memory management, we don’t have any guarantees that the
      // following works as intended, but it should in most cases:
      Arrays.fill(iv, (byte) 0); //overwrite the content of key with zeros

      return EncryptedSecret.builder()
        .keyMetadata(keyMetadata)
        .encryptionAlgorithm(EncryptionAlgorithm.AES_GCM)
        .cipherMessage(cipherMessage)
        .build();
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
      InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
      throw new EncryptionException("Unable to Encrypt: ", e);
    }
  }

  @Override
  public byte[] decrypt(KeyMetadata keyMetadata, EncryptionAlgorithm encryptionAlgorithm, byte[] cipherMessage) {
    Objects.requireNonNull(keyMetadata);
    Objects.requireNonNull(encryptionAlgorithm);
    Objects.requireNonNull(cipherMessage);

    // First, deconstruct the message
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
      int ivLength = byteBuffer.getInt();
      if (ivLength < 12 || ivLength >= 16) { // check input parameter
        throw new IllegalArgumentException("invalid iv length");
      }
      byte[] iv = new byte[ivLength];
      byteBuffer.get(iv);
      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);

      final Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
      byte[] plainText = cipher.doFinal(cipherText);
      return plainText;
    } catch (Exception e) {
      throw new EncryptionException(e.getMessage(), e);
    }

  }

}
