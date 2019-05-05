package org.interledger.crypto;

public class EncryptionException extends CryptoException {
  public EncryptionException() {
  }

  public EncryptionException(String message) {
    super(message);
  }

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }

  public EncryptionException(Throwable cause) {
    super(cause);
  }
}
