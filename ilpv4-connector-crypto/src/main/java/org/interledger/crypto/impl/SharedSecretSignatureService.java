package org.interledger.crypto.impl;

import org.interledger.crypto.KeyMetadata;
import org.interledger.crypto.SignatureAlgorithm;
import org.interledger.crypto.SignatureException;
import org.interledger.crypto.SignatureService;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * A {@link SignatureService} that signs and verifies signatures using a shared-secret.
 */
public class SharedSecretSignatureService implements SignatureService {

  private final byte[] sharedSecretBytes;

  public SharedSecretSignatureService(final byte[] sharedSecretBytes) {
    this.sharedSecretBytes = Objects.requireNonNull(sharedSecretBytes);
  }

  @Override
  public byte[] sign(KeyMetadata keyMetadata, SignatureAlgorithm signatureAlgorithm, byte[] data) throws SignatureException {
    if (signatureAlgorithm.equals(SignatureAlgorithm.HMAC_SHA_256)) {
      try {
        final Mac mac = Mac.getInstance(signatureAlgorithm.getAlgorithm());
        final SecretKey macKey = new SecretKeySpec(sharedSecretBytes, signatureAlgorithm.getAlgorithm());
        mac.init(macKey);
        final byte[] digest = mac.doFinal(data);
        return digest;
      } catch (Exception e) {
        throw new SignatureException("unable to sign", e);
      }
    } else {
      throw new SignatureException("Unsupported SignatureAlgorithm: " + signatureAlgorithm);
    }
  }

  @Override
  public boolean verify(KeyMetadata keyMetadata, SignatureAlgorithm signatureAlgorithm, byte[] data, byte[] signature)
    throws SignatureException {
    return MessageDigest.isEqual(sign(keyMetadata, signatureAlgorithm, data), signature);
  }
}
