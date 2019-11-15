package org.interledger.connector.server.spring.auth.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.links.DefaultLinkSettingsFactory;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.server.spring.settings.ConnectorSettingsFromPropertyFileTest;
import org.interledger.connector.server.spring.settings.crypto.JksCryptoConfig;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ModifiableConnectorSettings;
import org.interledger.crypto.EncryptionService;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;

import com.google.common.hash.HashCode;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Base64;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {JksCryptoConfig.class, ConnectorSettingsFromPropertyFileTest.TestConfiguration.class}
)
@ActiveProfiles("connector-unit-test")
public class IlpOverHttpAuthenticationProviderTest {

  private static final String SECRET = Base64.getEncoder().encodeToString("shh".getBytes());
  private static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  private static final String JWT_TOKEN =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJib2IifQ.E773YFqatHhCSBQKp8kkqpdqFpFf5DkRxdDR35pd67M";

  private static final String SIMPLE_TOKEN = "bob:" + SECRET;

  public static final String ILP_OVER_HTTP_INCOMING =
    IlpOverHttpLinkSettings.ILP_OVER_HTTP + "." + IlpOverHttpLinkSettings.INCOMING + ".";
  public static final String ILP_OVER_HTTP_OUTGOING =
    IlpOverHttpLinkSettings.ILP_OVER_HTTP + "." + IlpOverHttpLinkSettings.OUTGOING + ".";

  public static final AccountId ACCOUNT_ID = AccountId.of("bob");

  private IlpOverHttpAuthenticationProvider ilpOverHttpAuthenticationProvider;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private AccountSettingsRepository accountSettingsRepository;
  @Mock
  private CacheMetricsCollector cacheMetrics;

  @Autowired
  private EncryptionService encryptionService;

  @Before
  public void setUp() {
    initMocks(this);
    ConnectorSettings connectorSettings = ModifiableConnectorSettings.create();
    ilpOverHttpAuthenticationProvider = new IlpOverHttpAuthenticationProvider(
      () -> connectorSettings,
      encryptionService,
      accountSettingsRepository,
      new DefaultLinkSettingsFactory(),
      cacheMetrics);
  }

  @Test
  public void authenticateSimpleWithValidToken() {
    mockAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString());
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(SIMPLE_TOKEN.getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getPrincipal()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void authenticateSimpleWithInvalidSecret() {
    mockAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString());
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Authentication failed for principal: bob");
    ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken("bob:badtoken".getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );
  }

  @Test
  public void authenticateSimpleWithInvalidPrincipal() {
    mockAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString());
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Authentication failed for principal");
    ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(("foo:" + SECRET).getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build());
  }

  @Test
  public void authenticateJwtWithValidToken() {
    mockAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.JWT_HS_256.toString());
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(JWT_TOKEN.getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getPrincipal()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void authenticateJwtWithInvalidToken() {
    mockAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.JWT_HS_256.toString());

    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Authentication failed for principal");
    ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken("not a jwt".getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );
  }

  @Test
  public void authenticateInternalServerError() {
    mockAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.JWT_HS_256.toString());

    when(accountSettingsRepository.findByAccountIdWithConversion(any())).thenThrow(
      new RuntimeException("Something bad happened on the server")
    );

    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Unable to validate token due to system error");
    ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(JWT_TOKEN.getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );
  }

  private ImmutableAccountSettings.Builder mockAccountSettings() {
    ImmutableAccountSettings.Builder builder = AccountSettings.builder()
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("XRP")
      .assetScale(9)
      .accountId(ACCOUNT_ID)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.URL, "http://test.com")
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.TOKEN_SUBJECT, ACCOUNT_ID.value())
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.URL, "http://test.com")
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString())
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.TOKEN_SUBJECT, ACCOUNT_ID.value());

    when(accountSettingsRepository.findByAccountIdWithConversion(ACCOUNT_ID))
      .thenAnswer(($) -> Optional.of(builder.build()));

    return builder;
  }
}
