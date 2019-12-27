package org.interledger.connector.server.spring.auth.ilpoverhttp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Verification;
import com.auth0.spring.security.api.JwtAuthenticationProvider;
import com.auth0.spring.security.api.authentication.JwtAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * An extension of the Auth0 {@link AuthenticationProvider} that implements the `JWT_HS_256` Authentication profile
 * defined in IL-RFC-35. This implementation closely mirrors {@link JwtAuthenticationProvider} except that it allows for
 * null `iss` and `aud` claims.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
public class JwtRs256AuthenticationProvider implements AuthenticationProvider {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final JwtRs256Configuration configuration;

  private long leeway = 0L;

  public JwtRs256AuthenticationProvider(JwtRs256Configuration configuration) {
    this.configuration = configuration;
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
        final Authentication jwtAuth = jwt.verify(this.newJWTVerifier());
        logger.debug("Authenticated jwt with scopes {}", jwtAuth.getAuthorities());
        return jwtAuth;
      } catch (JWTVerificationException var4) {
        throw new BadCredentialsException("Not a valid token", var4);
      }
    }
  }

  private JWTVerifier newJWTVerifier() {
    Verification verifier = JWT.require(Algorithm.RSA256(configuration.keyProvider()))
      .acceptLeeway(leeway)
      .withSubject(configuration.subject())
      .withIssuer(configuration.issuer().toString())
      .withAudience(configuration.audience());
    return verifier.build();
  }

}
