package org.interledger.secrets.gcp;

/**
 * Defines supported encryption algorithms.
 */
public enum GcpEncryptionAlgorithm {

  // Google KMS
  GOOGLE_SYMMETRIC("GS");

  private final String encodedValue;

  GcpEncryptionAlgorithm(final String encodedValue) {
    this.encodedValue = encodedValue;
  }

  static GcpEncryptionAlgorithm fromEncodedValue(final String encodedValue) {
    switch (encodedValue) {
      case "GS": {
        return GOOGLE_SYMMETRIC;
      }
      default: {
        throw new RuntimeException("Invalid Encryption Algorithm: " + encodedValue);
      }
    }
  }
}
