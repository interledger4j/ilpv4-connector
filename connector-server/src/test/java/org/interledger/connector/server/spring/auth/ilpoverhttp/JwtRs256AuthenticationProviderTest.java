package org.interledger.connector.server.spring.auth.ilpoverhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.server.JwksServer;
import org.interledger.link.http.ImmutableJwtAuthSettings;
import org.interledger.link.http.JwtAuthSettings;

import com.auth0.jwk.UrlJwkProvider;
import com.auth0.spring.security.api.authentication.PreAuthenticatedAuthenticationJsonWebToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;

public class JwtRs256AuthenticationProviderTest {

  public static final String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  private JwksServer jwtServer;

  @Before
  public void setUp() throws JsonProcessingException {
    jwtServer = new JwksServer();
    resetJwks();
  }

  @Test
  public void authenticate() {
    HttpUrl issuer = HttpUrl.parse(wireMockRule.baseUrl());
    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);

    JwtRs256Configuration configuration = defaultJwtRs256Config(jwtAuthSettings);

    String jwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(10));
    PreAuthenticatedAuthenticationJsonWebToken jwtAuth =
      PreAuthenticatedAuthenticationJsonWebToken.usingToken(jwt);

    assertThat(new JwtRs256AuthenticationProvider(configuration).authenticate(jwtAuth).isAuthenticated()).isTrue();
    verify(exactly(1), getRequestedFor(urlEqualTo(WELL_KNOWN_JWKS_JSON)));
  }

  @Test
  public void authenticateDifferentKeysMultipleTimes() {
    HttpUrl issuer = HttpUrl.parse(wireMockRule.baseUrl());
    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);

    JwtRs256Configuration configuration = defaultJwtRs256Config(jwtAuthSettings);

    for (int keyId = 0; keyId < jwtServer.getKeyCount(); keyId++) {
      String jwt = jwtServer.createJwtUsingKey(jwtAuthSettings, Instant.now().plusSeconds(10), keyId);
      PreAuthenticatedAuthenticationJsonWebToken jwtAuth =
        PreAuthenticatedAuthenticationJsonWebToken.usingToken(jwt);
      assertThat(new JwtRs256AuthenticationProvider(configuration).authenticate(jwtAuth).isAuthenticated()).isTrue();
    }

    verify(exactly(jwtServer.getKeyCount()), getRequestedFor(urlEqualTo(WELL_KNOWN_JWKS_JSON)));
  }

  @Test
  public void authenticateFailsOnMismatchedKeys() throws JsonProcessingException {
    HttpUrl issuer = HttpUrl.parse(wireMockRule.baseUrl());
    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);

    String jwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(10));

    resetJwks();

    JwtRs256Configuration configuration = defaultJwtRs256Config(jwtAuthSettings);

    PreAuthenticatedAuthenticationJsonWebToken jwtAuth = PreAuthenticatedAuthenticationJsonWebToken.usingToken(jwt);

    expectedException.expect(BadCredentialsException.class);
    new JwtRs256AuthenticationProvider(configuration).authenticate(jwtAuth);
  }

  @Test
  public void authenticateFailsOnExpiredJwt() throws JsonProcessingException {
    HttpUrl issuer = HttpUrl.parse(wireMockRule.baseUrl());
    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);
    String expiredJwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().minusSeconds(10));

    JwtRs256Configuration configuration = defaultJwtRs256Config(jwtAuthSettings);
    PreAuthenticatedAuthenticationJsonWebToken jwtAuth =
      PreAuthenticatedAuthenticationJsonWebToken.usingToken(expiredJwt);

    expectedException.expect(BadCredentialsException.class);
    new JwtRs256AuthenticationProvider(configuration).authenticate(jwtAuth);
  }

  private JwtRs256Configuration defaultJwtRs256Config(JwtAuthSettings jwtAuthSettings) {
    HttpUrl issuer = jwtAuthSettings.tokenIssuer().get();
    return JwtRs256Configuration.builder()
      .subject(jwtAuthSettings.tokenSubject())
      .audience(jwtAuthSettings.tokenAudience().get())
      .issuer(issuer)
      .keyProvider(new JwkRsaPublicKeyProvider(new UrlJwkProvider(issuer.toString())))
      .build();
  }

  private ImmutableJwtAuthSettings defaultAuthSettings(HttpUrl issuer) {
    return JwtAuthSettings.builder()
      .tokenIssuer(issuer)
      .tokenSubject("foo")
      .tokenAudience("bar")
      .build();
  }

  private void resetJwks() throws JsonProcessingException {
    jwtServer.resetKeyPairs();
    WireMock.reset();
    stubFor(get(urlEqualTo(WELL_KNOWN_JWKS_JSON))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(objectMapper.writeValueAsString(jwtServer.getJwks()))
      ));
  }

}