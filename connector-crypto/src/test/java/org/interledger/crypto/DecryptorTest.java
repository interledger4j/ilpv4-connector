package org.interledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;

public class DecryptorTest {

  // dummy Decryptor for testing helper functions. Decryption just returns a byte array with zeroes
  private Decryptor zeroDecryptor = (keyMetadata, encryptionAlgorithm, cipherMessage) -> {
    byte[] decrypted = new byte[cipherMessage.length];
    Arrays.fill(decrypted, (byte) 0);
    return decrypted;
  };

  @Test
  public void withDecrypted() {
    String secret = Base64.getEncoder().encodeToString("shh".getBytes());
    byte[] expected = new byte[] {0, 0, 0};
    byte[] notExpected = new byte[] {1, 1, 1};

    EncryptedSecret encryptedSecret =
        EncryptedSecret.fromEncodedValue("enc:JKS:zero:test:test:aes_gcm:" + secret);

    assertThat(zeroDecryptor.isEqualDecrypted(encryptedSecret, expected)).isTrue();
    assertThat(zeroDecryptor.isEqualDecrypted(encryptedSecret, notExpected)).isFalse();
  }

}