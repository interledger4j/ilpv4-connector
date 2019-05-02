package org.interledger.crypto;

/**
 * Defines supported encryption algorithms.
 */
public enum EncryptionAlgorithm {

  // Google KMS
  GOOGLE_SYMMETRIC(CryptoConstants.GOOGLE_SYMMETRIC),
  AES_GCM(CryptoConstants.AES_GCM);

  private final String algorithm;

  EncryptionAlgorithm(final String algorithm) {
    this.algorithm = algorithm;
  }

  static EncryptionAlgorithm fromEncodedValue(final String encodedValue) {
    switch (encodedValue) {
      case CryptoConstants.GOOGLE_SYMMETRIC: {
        return GOOGLE_SYMMETRIC;
      }
      case CryptoConstants.AES_GCM: {
        return AES_GCM;
      }
      default: {
        throw new RuntimeException("Invalid Encryption Algorithm: " + encodedValue);
      }
    }
  }

  public String getAlgorithm() {
    return algorithm;
  }
}
