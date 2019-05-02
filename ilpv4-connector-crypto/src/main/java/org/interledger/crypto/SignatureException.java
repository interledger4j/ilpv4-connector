package org.interledger.crypto;

public class SignatureException extends CryptoException {
  public SignatureException() {
  }

  public SignatureException(String message) {
    super(message);
  }

  public SignatureException(String message, Throwable cause) {
    super(message, cause);
  }

  public SignatureException(Throwable cause) {
    super(cause);
  }
}
