package org.interledger.crypto;

/**
 * Verify signatures using keys stored in arbitrary KMS systems.
 */
public interface SignatureVerifier {

  /**
   * Verify a signature
   *
   * @param keyMetadata        The {@link KeyMetadata} for this operation.
   * @param signatureAlgorithm The {@link SignatureAlgorithm} for this operation.
   * @param data               The data that was signed to produce {@code signature}.
   * @param signature          The computed signature to compare against.
   *
   * @return {@code true} if the signature is valid; {@code false} otherwise.
   *
   * @throws SignatureException if anything goes wrong while attempting to verify the supplied {@code data}.
   */
  boolean verify(KeyMetadata keyMetadata, SignatureAlgorithm signatureAlgorithm, byte[] data, byte[] signature)
    throws SignatureException;
}
