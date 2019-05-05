package com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast;

import com.auth0.spring.security.api.JwtAuthenticationProvider;
import org.interledger.connector.link.blast.tokenSettings.SharedSecretTokenSettings;
import org.interledger.crypto.Decryptor;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * <p>An {@link AuthenticationProvider} that accepts a JWT_HS_256 Bearer token (as configured in Link settings) for a
 * particular account.</p>
 *
 * <p>Note that this implementation wraps an individual instance of {@link JwtAuthenticationProvider} for each
 * BLAST peer that authentication is required to be performed with.</p>
 */
public abstract class BlastAuthenticationProvider implements AuthenticationProvider {

  /**
   * Load the shared-secret by decrypting it using the currently configured {@link Decryptor}.
   *
   * @param sharedSecretTokenSettings The {@link SharedSecretTokenSettings} to lookup the shared secret.
   *
   * @return A {@link String} representing the encoded secret that contains an encrypted shared-secret.
   **/
  protected abstract byte[] determineSharedSecret(final SharedSecretTokenSettings sharedSecretTokenSettings);
}
