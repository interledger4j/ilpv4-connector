package org.interledger.connector.server.spring.auth.ilpoverhttp;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.interfaces.RSAKeyProvider;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class JwkRsaPublicKeyProvider implements RSAKeyProvider {

  private final JwkProvider jwkProvider;

  public JwkRsaPublicKeyProvider(JwkProvider jwkProvider) {
    this.jwkProvider = jwkProvider;
  }

  @Override
  public RSAPublicKey getPublicKeyById(String keyId) {
    try {
      return (RSAPublicKey) jwkProvider.get(keyId).getPublicKey();
    } catch (JwkException e) {
      throw new IllegalStateException("could fetch jwk for key id " + keyId, e);
    }
  }

  @Override
  public RSAPrivateKey getPrivateKey() {
    throw new UnsupportedOperationException("private keys not supported");
  }

  @Override
  public String getPrivateKeyId() {
    throw new UnsupportedOperationException("private keys not supported");
  }
}
