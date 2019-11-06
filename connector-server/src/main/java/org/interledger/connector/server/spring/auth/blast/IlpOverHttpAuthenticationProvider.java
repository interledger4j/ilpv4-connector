package org.interledger.connector.server.spring.auth.blast;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.SharedSecretTokenSettings;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.spring.security.api.authentication.JwtAuthentication;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>An {@link AuthenticationProvider} that implements the Authentication profiles defined in IL-RFC-35.</p>
 *
 * <p>This implementation treats shared-secrets as sensitive values because they are an actual authentication
 * credential and can be used to generate new authentication tokens. Thus, this implementation attempts to reduce the
 * amount of time (to as short of a window of time as possible) that this information is stored in-memory in unencrypted
 * form.</p>
 *
 * <p>To do this, authentication decisions for any particular token are cached in-memory for a small duration. However,
 * if there is no authentication decision in the cache, then this provider will delegate to an underlying implementation
 * that will actually load the credentials for an incoming connection (currently these are stored in encrypted format on
 * each account) and then decrypt that value in order to verify an incoming token. The authentication decision is then
 * cached per the above. In this way, the actual shared-secret is only ever kept in-memory briefly during a particular
 * token verification, but is otherwise discarded from memory quickly.</p>
 *
 * <p>Note that the cache expiry of an authentication decision will be extended after every request that uses the same
 * token, so a cache expiry will not occur until after X minutes have elapsed with no requests using a given
 * token).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class IlpOverHttpAuthenticationProvider implements AuthenticationProvider {

  private static final String AUTH_DECISIONS_CACHE_NAME = "ilpOverHttpAuthenticationDecisionsCache";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Decryptor decryptor;
  //  Used to construct an HMAC of an authentication token (note that in `JWT_HS_256`, the token itself is derived
  //  from an underlying shared-secret, but the token is still sensitive since it is a bearer-auth instrument. Using
  //  this construction, the implementation can trivially determine if request's token has been authenticated by the
  //  cache without actually caching the token itself. The rationale here is that if an attacker were to perform a
  //  memory dump, then the attacker would only be able to grab only derivatives of actual tokens (which are useless),
  //  and would not be able to replay actual requests nor generate new tokens.
  private final byte[] ephemeralHmacBytes;
  // See Javadoc above for how this is used.
  private final LoadingCache<AuthenticationRequest, AuthenticationDecision> ilpOverHttpAuthenticationDecisions;

  public IlpOverHttpAuthenticationProvider(
      final Supplier<ConnectorSettings> connectorSettingsSupplier,
      final Decryptor decryptor,
      final AccountSettingsRepository accountSettingsRepository,
      final LinkSettingsFactory linkSettingsFactory,
      final CacheMetricsCollector cacheMetrics
  ) {
    Objects.requireNonNull(connectorSettingsSupplier);

    this.ephemeralHmacBytes = this.generate32RandomBytes();
    this.decryptor = Objects.requireNonNull(decryptor);

    ilpOverHttpAuthenticationDecisions = Caffeine.newBuilder()
        .recordStats() // Publish stats to prometheus
        .maximumSize(5000)
        // Expire after this duration, which will correspond to the last incoming request from the peer.
        // TODO: This value should be configurable and match the server-global token expiry.
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .removalListener((RemovalListener<AuthenticationRequest, AuthenticationDecision>)
            (authenticationRequest, authenticationDecision, cause) ->
                logger.debug("Removing IlpOverHttp AuthenticationDecision from Cache for Principal: {}",
                    authenticationDecision.principal()))
        .build(
            authenticationRequest -> {
              Objects.requireNonNull(authenticationRequest);

              try {
                final AccountId authPrincipal =
                    AccountId.of(authenticationRequest.incomingAuthentication().getPrincipal().toString());
                final AccountSettings accountSettings =
                    accountSettingsRepository.findByAccountIdWithConversion(authPrincipal)
                        .orElseThrow(() -> new AccountNotFoundProblem(authPrincipal));

                // Discover the BlastSettings
                final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
                    Objects.requireNonNull(linkSettingsFactory).constructTyped(accountSettings);
                final IlpOverHttpLinkSettings.AuthType authType = ilpOverHttpLinkSettings.incomingHttpLinkSettings()
                    .authType();

                // This implementation performs the same logic for either token type. In order to utilize SIMPLE in this
                // scheme, the peer should be given a very-long duration JWT token which it can use as a SIMPLE Bearer
                // token.
                if (
                    IlpOverHttpLinkSettings.AuthType.JWT_HS_256.equals(authType) ||
                        IlpOverHttpLinkSettings.AuthType.SIMPLE.equals(authType)
                ) {
                  final IncomingLinkSettings incomingLinkSettings = ilpOverHttpLinkSettings.incomingHttpLinkSettings();

                  // These bytes will be zeroed-out immediately after making the auth decision.
                  final byte[] sharedSecretBytes = decryptSharedSecret(authPrincipal, incomingLinkSettings);
                  final byte[] sharedSecretBytesHmac =
                      Hashing.hmacSha256(ephemeralHmacBytes).hashBytes(sharedSecretBytes).asBytes();

                  try {
                    // Even though the bytes are passed-into this provider, the bytes are zeroed out below which render
                    // this provider unusable after this method. Because the provider is only ever used once, it can
                    // be garbage collected after this method is finished.
                    final Authentication jwtAuthResult = new JwtHs256AuthenticationProvider(sharedSecretBytes)
                        .authenticate(authenticationRequest.incomingAuthentication());

                    if (logger.isDebugEnabled()) {
                      logger.debug(
                          "JwtHs256AuthenticationProvider returned with an AuthResult: {}",
                          jwtAuthResult.isAuthenticated()
                      );
                    }

                    return AuthenticationDecision.builder()
                        .authentication(jwtAuthResult)
                        .credentialHmac(sharedSecretBytesHmac)
                        .build();
                  } finally {
                    // Zero-out all bytes in the `sharedSecretBytes` array.
                    Arrays.fill(sharedSecretBytes, (byte) 0);
                  }

                } else {
                  throw new RuntimeException("Unsupported AuthType: " + authType);
                }
              } catch (AccountNotFoundProblem e) { // All other exceptions should be thrown!
                logger.debug(e.getMessage(), e);
                // Cache this result and return it if an exception was encountered. Note that the authenticationRequest
                // always returns false for isAuthenticated, which is what we want here.
                return AuthenticationDecision.builder()
                    .authentication(authenticationRequest.incomingAuthentication())
                    .credentialHmac(new byte[32])
                    .build();
              }
            });

    Objects.requireNonNull(cacheMetrics).addCache(AUTH_DECISIONS_CACHE_NAME, ilpOverHttpAuthenticationDecisions);
  }

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    try {
      final AuthenticationDecision result = ilpOverHttpAuthenticationDecisions.get(
          AuthenticationRequest.builder().incomingAuthentication(authentication).build()
      );

      if (result.isAuthenticated()) {
        return result.authentication();
      } else {
        throw new BadCredentialsException("Invalid token for Principal: " + authentication.getPrincipal());
      }
    } catch (BadCredentialsException e) {
      if (e.getCause() != null && (InvalidClaimException.class.isAssignableFrom(e.getCause().getClass()) ||
          SignatureVerificationException.class.isAssignableFrom(e.getCause().getClass()))) {
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
   * Decrypts the shared-secret from {@link SharedSecretTokenSettings#encryptedTokenSharedSecret()} and returns it as a
   * byte-array.
   *
   * @param authPrincipal             An {@link AccountId} to decrypt a shared secret for.
   * @param sharedSecretTokenSettings A {@link SharedSecretTokenSettings} to use while decrypting a shared secret.
   *
   * @return The actual underlying shared-secret.
   */
  @VisibleForTesting
  protected final byte[] decryptSharedSecret(
      final AccountId authPrincipal, final SharedSecretTokenSettings sharedSecretTokenSettings
  ) {
    Objects.requireNonNull(authPrincipal);
    Objects.requireNonNull(sharedSecretTokenSettings);

    return Optional.of(sharedSecretTokenSettings)
        .map(SharedSecretTokenSettings::encryptedTokenSharedSecret)
        .map(EncryptedSecret::fromEncodedValue)
        .map(decryptor::decrypt)
        .orElseThrow(() -> new BadCredentialsException(String.format("No account found for `%s`", authPrincipal)));
  }

  /**
   * Generate 32 random bytes that can be used as an ephemeral HMAC key. This key is only used to Hash actual
   * shared-secret values that are stored in an in-memory cache. If this server goes away, this this cache will go away
   * too, so this secret key can be ephemeral.
   *
   * Note too that the "values" being HMAC'd are also not in memory, so re-creating them using just this ephemeral value
   * would not be possible.
   */
  private byte[] generate32RandomBytes() {
    final SecureRandom secureRandom = new SecureRandom();
    final byte[] rndBytes = new byte[32];
    secureRandom.nextBytes(rndBytes);
    return rndBytes;
  }

}
