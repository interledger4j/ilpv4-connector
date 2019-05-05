package com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.spring.security.api.JwtAuthenticationProvider;
import com.auth0.spring.security.api.authentication.JwtAuthentication;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sappenin.interledger.ilpv4.connector.links.LinkSettingsFactory;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.tokenSettings.SharedSecretTokenSettings;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>An {@link AuthenticationProvider} that accepts a JWT_HS_256 Bearer token (as configured in Link settings) for a
 * particular account.</p>
 *
 * <p>Note that this implementation wraps an individual instance of {@link JwtAuthenticationProvider} for each
 * BLAST peer that authentication is required to be performed with.</p>
 */
public class JwtBlastAuthenticationProvider extends BlastAuthenticationProvider implements AuthenticationProvider {

  //private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Decryptor decryptor;
  private final LoadingCache<AccountId, JwtAuthenticationProvider> jwtAuthenticationProviders;

  public JwtBlastAuthenticationProvider(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final Decryptor decryptor,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkSettingsFactory linkSettingsFactory
  ) {
    Objects.requireNonNull(connectorSettingsSupplier);
    this.decryptor = Objects.requireNonNull(decryptor);

    // TODO: We don't want to construct new instances of JwtVerifier for every account connection, but for now we
    //  don't want to replicate all the logic of the underlying library, so we just use a cache to help reduce
    //  instantiations.
    jwtAuthenticationProviders = CacheBuilder.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .build(
        new CacheLoader<AccountId, JwtAuthenticationProvider>() {
          public JwtAuthenticationProvider load(final AccountId accountId) {
            Objects.requireNonNull(accountId);
            final AccountSettingsEntity accountSettings = accountSettingsRepository.safeFindByAccountId(accountId);

            // Discover the BlastSettings
            final BlastLinkSettings blastLinkSettings =
              Objects.requireNonNull(linkSettingsFactory).constructTyped(accountSettings);
            final BlastLinkSettings.AuthType authType = blastLinkSettings.incomingBlastLinkSettings().authType();

            if (BlastLinkSettings.AuthType.JWT_HS_256.equals(authType)) {
              final IncomingLinkSettings incomingLinkSettings = blastLinkSettings.incomingBlastLinkSettings();
              return new JwtAuthenticationProvider(
                // secret
                determineSharedSecret(incomingLinkSettings),
                // The issuer of the token is generally the URL of the remote connector.
                incomingLinkSettings.tokenIssuer()
                  .map(HttpUrl::toString)
                  .orElseThrow(() -> new RuntimeException("Blast Auth requires an Incoming Token Issuer!")),
                // The audience of the token is generally _this_ connector.
                incomingLinkSettings.tokenAudience()
              );
            } else {
              throw new RuntimeException("Unsupported AuthType: " + authType);
            }
          }
        });
  }

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    try {
      final String token = authentication.getCredentials().toString();
      return jwtAuthenticationProviders.getUnchecked(AccountId.of(JWT.decode(token).getSubject()))
        .authenticate(authentication);
    } catch (BadCredentialsException e) {
      if (InvalidClaimException.class.isAssignableFrom(e.getCause().getClass()) ||
        SignatureVerificationException.class.isAssignableFrom(e.getCause().getClass())) {
        throw new BadCredentialsException(String.format("Not a valid token (%s)", e.getCause().getMessage()), e);
      } else {
        throw e;
      }
    } catch (Exception e) {
      if (BadCredentialsException.class.isAssignableFrom(e.getCause().getClass())) {
        throw e;
      } else {
        throw new BadCredentialsException("Not a valid Token", e);
      }
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthentication.class.isAssignableFrom(authentication);
  }

  /**
   * Load the shared-secret by decrypting it using the currently configured {@link Decryptor}.
   *
   * @param sharedSecretTokenSettings The {@link SharedSecretTokenSettings} to lookup the shared secret.
   *
   * @return A {@link String} representing the encoded secret that contains an encrypted shared-secret.
   **/
  protected final byte[] determineSharedSecret(final SharedSecretTokenSettings sharedSecretTokenSettings) {
    Objects.requireNonNull(sharedSecretTokenSettings);

    return Optional.of(sharedSecretTokenSettings)
      .map(SharedSecretTokenSettings::encryptedTokenSharedSecret)
      .map(EncryptedSecret::fromEncodedValue)
      .map(decryptor::decrypt)
      .orElseThrow(() -> new BadCredentialsException(
        String.format("No account found for `%s`", sharedSecretTokenSettings.tokenSubject())
      ));
  }
}
