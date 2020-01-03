package org.interledger.connector.server.spring.auth.ilpoverhttp;

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
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.link.http.SimpleAuthSettings;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.spring.security.api.JwtAuthenticationProvider;
import com.auth0.spring.security.api.authentication.PreAuthenticatedAuthenticationJsonWebToken;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
  private Set<IlpOverHttpLinkSettings.AuthType> supportedJwtAuthTypes =
    Sets.newHashSet(IlpOverHttpLinkSettings.AuthType.JWT_HS_256, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
  private final LoadingCache<HttpUrl, JwkProvider> jwkProviderCache;

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

    jwkProviderCache = Caffeine.newBuilder()
      .recordStats()
      .maximumSize(100)
      .build((httpUrl) -> new GuavaCachedJwkProvider(new UrlJwkProvider(httpUrl.url())));

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
          throw new BadCredentialsException("Authentication failed for principal: " + result.getPrincipal());
        }
      } else {
        logger.debug("Unsupported authentication type: " + authentication.getClass());
        return null;
      }
    } catch (AccountNotFoundProblem e) {
      throw new BadCredentialsException("Account not found for principal: " + authentication.getPrincipal());
    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      if (e.getCause() != null && BadCredentialsException.class.isAssignableFrom(e.getCause().getClass())) {
        throw e;
      } else {
        throw new BadCredentialsException("Unable to validate token due to system error", e);
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
    return authenticationDecisions.get(bearerAuth.hmacSha256(), (request) -> {
      AccountId accountId = bearerAuth.getAccountId();
      IncomingLinkSettings incomingLinkSettings = getIncomingLinkSettings(accountId)
        .orElseThrow(() -> new IllegalArgumentException("no incoming settings for " + accountId));

      if (incomingLinkSettings.authType().equals(IlpOverHttpLinkSettings.AuthType.SIMPLE)) {
        return authenticateAsSimple(bearerAuth, accountId, incomingLinkSettings);
      } else {
        return authenticateAsJwt(bearerAuth, accountId, incomingLinkSettings);
      }
    });
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return BearerAuthentication.class.isAssignableFrom(authentication);
  }

  private AuthenticationDecision authenticateAsJwt(BearerAuthentication pendingAuth,
                                                   AccountId accountId,
                                                   IncomingLinkSettings incomingLinkSettings) {
    try {

      PreAuthenticatedAuthenticationJsonWebToken jwt =
        PreAuthenticatedAuthenticationJsonWebToken.usingToken(new String(pendingAuth.getBearerToken()));

      if (jwt == null) {
        throw new JWTDecodeException("jwt decoded to null. Value: " + new String(pendingAuth.getBearerToken()));
      }

      if (!supportedJwtAuthTypes.contains(incomingLinkSettings.authType())) {
        throw new IllegalStateException("JWT authentication not supported for auth type for incoming link with type "
          + incomingLinkSettings.authType());
      }

      JwtAuthSettings jwtAuthSettings = incomingLinkSettings.jwtAuthSettings()
        .orElseThrow(() ->
          missingJwtAuthSetting(accountId, "jwtAuthSettings"));

      switch (incomingLinkSettings.authType()) {
        case JWT_HS_256: {
          return authenticateWithJwtHs256(pendingAuth, jwt, accountId, jwtAuthSettings);
        }
        case JWT_RS_256: {
          return authenticateWithJwtRs256(pendingAuth, jwt, accountId, jwtAuthSettings);
        }
        default:
          throw new IllegalArgumentException(incomingLinkSettings.authType() + " not supported");
      }
    } catch (AccountNotFoundProblem | JWTDecodeException e) { // All other exceptions should be thrown!
      logger.debug(e.getMessage(), e);
    }
    return notAuthenticated();
  }

  private AuthenticationDecision authenticateWithJwtRs256(BearerAuthentication pendingAuth,
                                                          PreAuthenticatedAuthenticationJsonWebToken jwt,
                                                          AccountId accountId,
                                                          JwtAuthSettings jwtAuthSettings) {
    HttpUrl issuer = jwtAuthSettings.tokenIssuer()
      .orElseThrow(() -> missingJwtAuthSetting(accountId, "jwtAuthSettings.tokenIssuer"));

    HttpUrl jksUrl = new HttpUrl.Builder()
      .scheme(issuer.scheme())
      .host(issuer.host())
      .port(issuer.port())
      .addPathSegment(".well-known")
      .addPathSegment("jwks.json")
      .build();

    Authentication authResult = new JwtAuthenticationProvider(jwkProviderCache.get(jksUrl),
      issuer.toString(),
      jwtAuthSettings.tokenAudience()
        .orElseThrow(() -> missingJwtAuthSetting(accountId, "jwtAuthSettings.tokenAudience")))
      .authenticate(jwt);

    if (!jwt.getPrincipal().equals(jwtAuthSettings.tokenSubject())) {
      throw new BadCredentialsException("jwt subject " + jwt.getPrincipal()
        + " does not match expected " + jwtAuthSettings.tokenSubject());
    }

    return AuthenticationDecision.builder()
      .principal(accountId)
      .isAuthenticated(authResult.isAuthenticated())
      .credentialHmac(pendingAuth.hmacSha256())
      .build();
  }

  private AuthenticationDecision authenticateWithJwtHs256(BearerAuthentication pendingAuth,
                                                          PreAuthenticatedAuthenticationJsonWebToken jwt,
                                                          AccountId accountId,
                                                          JwtAuthSettings jwtAuthSettings) {
    EncryptedSecret encryptedSecret = jwtAuthSettings.encryptedTokenSharedSecret()
      .map(secret -> EncryptedSecret.fromEncodedValue(secret))
      .orElseThrow(() ->
        missingJwtAuthSetting(accountId, "jwtAuthSettings.encryptedTokenSharedSecret"));

    return decryptor.withDecrypted(encryptedSecret, decryptedSecret -> {
      Authentication authResult = new JwtHs256AuthenticationProvider(jwtAuthSettings.tokenSubject(), decryptedSecret)
        .authenticate(jwt);

      logger.debug("authenticationProvider returned with an AuthResult: {}", authResult.isAuthenticated());

      return AuthenticationDecision.builder()
        .principal(accountId)
        .isAuthenticated(authResult.isAuthenticated())
        .credentialHmac(pendingAuth.hmacSha256())
        .build();
    });
  }

  private IllegalStateException missingJwtAuthSetting(AccountId accountId, String property) {
    return new IllegalStateException("Missing " + property + " for account id " + accountId.value());
  }

  private AuthenticationDecision authenticateAsSimple(BearerAuthentication authentication,
                                                      AccountId accountId,
                                                      IncomingLinkSettings incomingLinkSettings) {
    try {
      SimpleCredentials simpleCredentials =
        getSimpleCredentials(authentication.getAccountId(), authentication.getBearerToken());

      if (!incomingLinkSettings.authType().equals(IlpOverHttpLinkSettings.AuthType.SIMPLE)) {
        throw new BadCredentialsException("SIMPLE auth not configured for account id " + accountId.value());
      }

      SimpleAuthSettings simpleAuthSettings = incomingLinkSettings.simpleAuthSettings()
        .orElseThrow(() ->
          new BadCredentialsException("No simple auth settings for account id " + accountId.value()));

      EncryptedSecret encryptedSecret = EncryptedSecret.fromEncodedValue(simpleAuthSettings.authToken());

      boolean isAuthenticated = decryptor.isEqualDecrypted(encryptedSecret, simpleCredentials.getAuthToken());

      return AuthenticationDecision.builder()
        .principal(accountId)
        .credentialHmac(authentication.hmacSha256())
        .isAuthenticated(isAuthenticated)
        .build();
    } catch (AccountNotFoundProblem e) { // All other exceptions should be thrown!
      logger.debug(e.getMessage(), e);
    }
    return notAuthenticated();
  }

  private Optional<IncomingLinkSettings> getIncomingLinkSettings(AccountId accountId) {
    final AccountSettings accountSettings =
      accountSettingsRepository.findByAccountIdWithConversion(accountId)
        .orElseThrow(() -> new AccountNotFoundProblem(accountId));
    final IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
      Objects.requireNonNull(linkSettingsFactory).constructTyped(accountSettings);

    return ilpOverHttpLinkSettings.incomingLinkSettings();
  }

  private static SimpleCredentials getSimpleCredentials(AccountId accountId, byte[] token) {
    return SimpleCredentials.builder()
      .principal(accountId)
      .authToken(token)
      .build();
  }

}
