package org.interledger.crypto;

import java.security.SecureRandom;

/**
 * Crypto utility methods for byte arrays.
 */
public class ByteArrayUtils {

  private static final SecureRandom secureRandom = new SecureRandom();

  /**
   * Checks if the byte arrays are equal using a constant-time algorithm to prevent timing based attacks.
   *
   * @param val1 first value to compare
   * @param val2 second value to compare
   *
   * @return true if val1 equals val2
   *
   * @see "https://codahale.com/a-lesson-in-timing-attacks"
   */
  public static boolean isEqualUsingConstantTime(byte[] val1, byte[] val2) {
    if (val1.length != val2.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < val1.length; i++) {
      result |= val1[i] ^ val2[i];
    }
    return result == 0;
  }

  /**
   * Generate 32 random bytes that can be used as an ephemeral HMAC key. This key is only used to Hash actual
   * shared-secret values that are stored in an in-memory cache. If this server goes away, this this cache will go away
   * too, so this secret key can be ephemeral.
   * <p>
   * Note too that the "values" being HMAC'd are also not in memory, so re-creating them using just this ephemeral value
   * would not be possible.
   */
  public static byte[] generate32RandomBytes() {
    final byte[] rndBytes = new byte[32];
    secureRandom.nextBytes(rndBytes);
    return rndBytes;
  }

}
