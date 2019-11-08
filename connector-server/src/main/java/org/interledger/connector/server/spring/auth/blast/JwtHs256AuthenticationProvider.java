package org.interledger.connector.server.spring.auth.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.spring.security.api.JwtAuthenticationProvider;
import com.auth0.spring.security.api.authentication.JwtAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

/**
 * An extension of the Auth0 {@link AuthenticationProvider} that implements the `JWT_HS_256` Authentication profile
 * defined in IL-RFC-35. This implementation closely mirrors {@link JwtAuthenticationProvider} except that it allows for
 * null `iss` and `aud` claims.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
public class JwtHs256AuthenticationProvider implements AuthenticationProvider {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // TODO: Remove these once https://github.com/interledger/rfcs/pull/531 is closed.
  private final Optional<String> issuer;
  private final Optional<String>  audience;

  private long leeway = 0L;
  private byte[] decryptedSharedSecret;

  public JwtHs256AuthenticationProvider(byte[] decryptedSharedSecret) {
    this.decryptedSharedSecret = decryptedSharedSecret;
    this.issuer = Optional.empty();
    this.audience = Optional.empty();
  }

  public JwtHs256AuthenticationProvider(byte[] decryptedSharedSecret,
                                        String issuer,
                                        String audience) {
    this.decryptedSharedSecret = decryptedSharedSecret;
    this.issuer = Optional.of(issuer);
    this.audience = Optional.of(audience);
  }

  private static JWTVerifier providerForHS256(byte[] secret, long leeway) {
    return JWT.require(Algorithm.HMAC256(secret))
        //.withIssuer(issuer)
        //.withAudience(new String[]{audience})
        .acceptLeeway(leeway).build();
  }

  public boolean supports(Class<?> authentication) {
    return JwtAuthentication.class.isAssignableFrom(authentication);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!this.supports(authentication.getClass())) {
      return null;
    } else {
      final JwtAuthentication jwt = (JwtAuthentication) authentication;
      try {
        final Authentication jwtAuth = jwt.verify(this.jwtVerifier(decryptedSharedSecret));
        logger.debug("Authenticated jwt with scopes {}", jwtAuth.getAuthorities());
        return jwtAuth;
      } catch (JWTVerificationException var4) {
        throw new BadCredentialsException("Not a valid token", var4);
      }
    }
  }

  private JWTVerifier jwtVerifier(byte[] secret) throws AuthenticationException {
    if (secret != null) {
      return providerForHS256(
          secret,
          //this.issuer,
          //this.audience,
          this.leeway);
    } else {
      throw new AuthenticationServiceException("Missing shared-secret!");
    }
  }

}
