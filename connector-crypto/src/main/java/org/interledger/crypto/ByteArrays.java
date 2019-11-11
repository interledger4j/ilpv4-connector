package org.interledger.crypto;

/**
 * Crypto utility methods for byte arrays.
 */
public class ByteArrays {

  /**
   * Checks if the byte arrays are equal using a constant-time algorithm to prevent timing based attacks
   * {@see https://codahale.com/a-lesson-in-timing-attacks/}
   * @param val1 first value to compare
   * @param val2 second value to compare
   * @return true if val1 equals val2
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


}
