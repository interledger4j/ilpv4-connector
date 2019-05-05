package org.interledger.crypto;

import static org.interledger.crypto.CryptoConstants.HMAC_SHA_256_VALUE;

/**
 * Defines supported signature algorithms.
 */
public enum SignatureAlgorithm {

  // HmacSHA256
  HMAC_SHA_256(HMAC_SHA_256_VALUE);

  private final String algorithm;

  SignatureAlgorithm(final String algorithm) {
    this.algorithm = algorithm;
  }

  static SignatureAlgorithm fromEncodedValue(final String encodedValue) {
    switch (encodedValue) {
      case HMAC_SHA_256_VALUE: {
        return HMAC_SHA_256;
      }
      default: {
        throw new RuntimeException("Invalid Signature Algorithm: " + encodedValue);
      }
    }
  }

  public String getAlgorithm() {
    return algorithm;
  }
}
