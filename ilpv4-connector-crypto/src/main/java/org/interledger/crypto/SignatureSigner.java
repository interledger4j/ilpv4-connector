package org.interledger.crypto;

/**
 * Create signatures using keys stored in arbitrary KMS systems.
 */
public interface SignatureSigner {
  /**
   * Sign {@code data} using underlying key-store.
   *
   * @param keyMetadata        The {@link KeyMetadata} for this operation.
   * @param signatureAlgorithm The {@link SignatureAlgorithm} for this operation.
   * @param data               A byte-array containing the arbitrary data to sign.
   *
   * @return A byte-array containing signed data.
   *
   * @throws SignatureException if anything goes wrong while attempting to sign the supplied {@code data}.
   */
  byte[] sign(KeyMetadata keyMetadata, SignatureAlgorithm signatureAlgorithm, byte[] data) throws SignatureException;
}

