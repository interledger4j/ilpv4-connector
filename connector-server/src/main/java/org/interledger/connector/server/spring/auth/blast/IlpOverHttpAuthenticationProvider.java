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

import com.auth0.spring.security.api.authentication.PreAuthenticatedAuthenticationJsonWebToken;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Base64;
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

  // See Javadoc above for how this is used.
  private final Cache<HashCode, AuthenticationDecision> authenticationDecisions;
  private AccountSettingsRepository accountSettingsRepository;
  private LinkSettingsFactory linkSettingsFactory;


  public IlpOverHttpAuthenticationProvider(
      final Supplier<ConnectorSettings> connectorSettingsSupplier,
      final Decryptor decryptor,
      final AccountSettingsRepository accountSettingsRepository,
      final LinkSettingsFactory linkSettingsFactory,
      final CacheMetricsCollector cacheMetrics
  ) {
    this.accountSettingsRepository = accountSettingsRepository;
    this.linkSettingsFactory = linkSettingsFactory;
    Objects.requireNonNull(connectorSettingsSupplier);
    this.decryptor = Objects.requireNonNull(decryptor);

    authenticationDecisions = Caffeine.newBuilder()
        .recordStats() // Publish stats to prometheus
        .maximumSize(5000)
        // Expire after this duration, which will correspond to the last incoming request from the peer.
        // TODO: This value should be configurable and match the server-global token expiry.
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .removalListener((RemovalListener<HashCode, AuthenticationDecision>)
            (authenticationRequest, authenticationDecision, cause) ->
                logger.debug("Removing IlpOverHttp AuthenticationDecision from Cache for Principal: {}",
                    authenticationDecision.getPrincipal()))
        .build();
    Objects.requireNonNull(cacheMetrics).addCache(AUTH_DECISIONS_CACHE_NAME, authenticationDecisions);
  }

  @Override
  public Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    try {
      if (authentication instanceof BearerAuthentication) {
        AuthenticationDecision result = authenticateBearer((BearerAuthentication) authentication);
        if (result.isAuthenticated()) {
          return result;
        } else {
          throw new BadCredentialsException("Invalid token for Principal: " + result.getPrincipal());
        }
      } else {
        logger.debug("Unsupported authentication type: " + authentication.getClass());
        return null;
      }
    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      if (e.getCause() != null && BadCredentialsException.class.isAssignableFrom(e.getCause().getClass())) {
        throw e;
      } else {
        throw new BadCredentialsException("Not a valid token", e);
      }
    }
  }

  private static AuthenticationDecision notAuthenticated() {
    // Cache this result and return it if an exception was encountered. Note that the authenticationRequest
    // always returns false for isAuthenticated, which is what we want here.
    return AuthenticationDecision.builder()
        .credentialHmac(HashCode.fromBytes(new byte[32]))
        .isAuthenticated(false)
        .build();
  }

  private AuthenticationDecision authenticateBearer(BearerAuthentication bearerAuth) {
    return authenticationDecisions.get(bearerAuth.hmacSha256(), (request) ->
        isSimple(bearerAuth.getBearerToken()) ? authenticateAsSimple(bearerAuth) :
        authenticateAsJwt(bearerAuth));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return BearerAuthentication.class.isAssignableFrom(authentication);
  }

  /**
   * Decrypts the shared-secret from {@link SharedSecretTokenSettings#encryptedTokenSharedSecret()} and returns it as a
   * byte-array.
   *
   * @param authPrincipal             An {@link AccountId} to decrypt a shared secret for.
   * @param sharedSecretTokenSettings A {@link SharedSecretTokenSettings} to use while decrypting a shared secret.
   * @return The actual underlying shared-secret.
   */
  @VisibleForTesting
  protected final EncryptedSecret getIncomingSecret(
      final AccountId authPrincipal, final SharedSecretTokenSettings sharedSecretTokenSettings
  ) {
    Objects.requireNonNull(authPrincipal);
    Objects.requireNonNull(sharedSecretTokenSettings);

    return Optional.of(sharedSecretTokenSettings)
        .map(SharedSecretTokenSettings::encryptedTokenSharedSecret)
        .map(EncryptedSecret::fromEncodedValue)
        .orElseThrow(() -> new BadCredentialsException(String.format("No account found for `%s`", authPrincipal)));
  }

  private AuthenticationDecision authenticateAsJwt(BearerAuthentication pendingAuth) {
    try {

      PreAuthenticatedAuthenticationJsonWebToken jwt =
          PreAuthenticatedAuthenticationJsonWebToken.usingToken(new String(pendingAuth.getBearerToken()));
      AccountId accountId = AccountId.of(jwt.getPrincipal().toString());
      EncryptedSecret encryptedSecret = getIncomingSecret(accountId);

      return decryptor.withDecrypted(encryptedSecret, decryptedSecret -> {
        Authentication authResult = new JwtHs256AuthenticationProvider(decryptedSecret)
            .authenticate(jwt);

        logger.debug("authenticationProvider returned with an AuthResult: {}", authResult.isAuthenticated());

        return AuthenticationDecision.builder()
            .principal(accountId)
            .isAuthenticated(authResult.isAuthenticated())
            .credentialHmac(pendingAuth.hmacSha256())
            .build();
      });
    } catch (AccountNotFoundProblem e) { // All other exceptions should be thrown!
      logger.error(e.getMessage(), e);
    }
    return notAuthenticated();
  }

  private AuthenticationDecision authenticateAsSimple(BearerAuthentication authentication) {
    try {
      SimpleCredentials simpleCredentials = getSimpleCredentials(authentication.getBearerToken())
          .orElseThrow(() -> new BadCredentialsException("invalid simple auth credentials"));

      EncryptedSecret encryptedSecret = getIncomingSecret(simpleCredentials.getPrincipal());

      boolean isAuthenticated = decryptor.isEqualDecrypted(encryptedSecret, simpleCredentials.getAuthToken());

      return AuthenticationDecision.builder()
          .principal(simpleCredentials.getPrincipal())
          .credentialHmac(authentication.hmacSha256())
          .isAuthenticated(isAuthenticated)
          .build();
    } catch (AccountNotFoundProblem e) { // All other exceptions should be thrown!
      logger.debug(e.getMessage(), e);
    }
    return notAuthenticated();
  }

  private EncryptedSecret getIncomingSecret(AccountId accountId) {
    final AccountSettings accountSettings =
        accountSettingsRepository.findByAccountIdWithConversion(accountId)
            .orElseThrow(() -> new AccountNotFoundProblem(accountId));
    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
        Objects.requireNonNull(linkSettingsFactory).constructTyped(accountSettings);

    final IncomingLinkSettings incomingLinkSettings = ilpOverHttpLinkSettings.incomingHttpLinkSettings();
    return getIncomingSecret(accountId, incomingLinkSettings);
  }

  private static Optional<SimpleCredentials> getSimpleCredentials(byte[] token) {
    String tokenString = new String(token);
    int tokenIndex = tokenString.lastIndexOf(":");
    if (tokenIndex > 0) {
      return Optional.of(SimpleCredentials.builder()
          .principal(AccountId.of(tokenString.substring(0, tokenIndex).trim()))
          .authToken(Base64.getDecoder().decode(tokenString.substring(tokenIndex+1).trim()))
          .build());
    }
    return Optional.empty();
  }

  private static boolean isSimple(byte[] token) {
    return new String(token).indexOf(":") > 0;
  }


}
