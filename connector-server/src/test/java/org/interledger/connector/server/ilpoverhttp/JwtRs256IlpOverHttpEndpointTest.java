package org.interledger.connector.server.ilpoverhttp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;
import static org.interledger.link.LoopbackLink.LOOPBACK_FULFILLMENT;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.routing.ExternalRoutingService;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.JwksServer;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.crypto.Decryptor;
import org.interledger.link.LinkId;
import org.interledger.link.LoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.ImmutableJwtAuthSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Ensures that the API endpoints for ILP-over-HTTP (i.e., `/ilp`) return the correct values for various test scenarios
 * using the `JWT_RS_256` profile defined in the Interledger HTTP Authentication recommendations RFC.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class JwtRs256IlpOverHttpEndpointTest extends AbstractEndpointTest {

  private static final String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";
  private static final String HUGH = "hugh";
  private static final String VIC = "vic";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private JwksServer jwtServer = new JwksServer();

  @LocalServerPort
  int randomServerPort;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  OkHttpClient okHttpClient;

  @Autowired
  Decryptor decryptor;

  @Autowired
  TestRestTemplate template;

  @Autowired
  ExternalRoutingService externalRoutingService;

  @Autowired
  ObjectMapper objectMapper;

  @Before
  public void setUp() throws JsonProcessingException {

    configureJwksEndpoint();

    if (!adminClient.findAccount(baseURI(), VIC).isPresent()) {
      final Map<String, Object> customSettings = Maps.newHashMap();
      customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, "JWT_RS_256 ");
      customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, "JWT_RS_256 ");
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, VIC);
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://vic.example.com");

      final AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of(VIC))
        .description("Loopback account for Alice using a JWT_RS_256")
        .accountRelationship(AccountRelationship.PEER)
        .linkType(LoopbackLink.LINK_TYPE)
        .customSettings(customSettings)
        .assetScale(2)
        .assetCode("XRP")
        .build();
      adminClient.createAccount(baseURI(), accountSettings);
    }

    if (!externalRoutingService.findBestNexHop(InterledgerAddress.of("test.connie.vic")).isPresent()) {
      externalRoutingService.createStaticRoute(
          StaticRoute.builder()
              .nextHopAccountId(AccountId.of("vic"))
              .routePrefix(InterledgerAddressPrefix.of("test.connie.vic"))
              .build()
      );
    }

    if (!externalRoutingService.findBestNexHop(InterledgerAddress.of("test.connie.hugh")).isPresent()) {
      externalRoutingService.createStaticRoute(
          StaticRoute.builder()
              .nextHopAccountId(AccountId.of("hugh"))
              .routePrefix(InterledgerAddressPrefix.of("test.connie.hugh"))
              .build()
      );
    }
  }

  private void configureJwksEndpoint() throws JsonProcessingException {
    stubFor(get(urlEqualTo(WELL_KNOWN_JWKS_JSON))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(objectMapper.writeValueAsString(jwtServer.getJwks()))
      ));
  }

  /**
   * Validate that Vic can send a packet to `hugh` and get a fulfillment back. Since `hugh` is operating a loopback
   * account, we always expect the {@link LoopbackLink#LOOPBACK_FULFILLMENT} to be returned, so we must make sure that
   * the prepare packets use the correct corresponding condition derived from that fulfillment.
   */
  @Test
  public void hughPaysVicUsingIlpOverHttp() {
    ImmutableJwtAuthSettings authSettings = JwtAuthSettings.builder()
      .tokenSubject(HUGH)
      .tokenAudience("foo")
      .tokenIssuer(HttpUrl.parse(wireMockRule.baseUrl()))
      .build();
    createAccount(AccountId.of(HUGH), customSettingsJwtRs256(
      authSettings
    ));
    String jwt = jwtServer.createJwt(authSettings, Instant.now().plusSeconds(30));
    final IlpOverHttpLink ilpOverHttpLink = ilpOverHttpLink(AccountId.of(HUGH), authSettings, jwt);

    ilpOverHttpLink.sendPacket(
      InterledgerPreparePacket.builder()
        .destination(InterledgerAddress.of("test.connie.vic"))
        .amount(UnsignedLong.ONE)
        .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
        .executionCondition(LOOPBACK_FULFILLMENT.getCondition())
        .build()
    ).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(LOOPBACK_FULFILLMENT),
      rejectPacket -> fail("Packet rejected but should not have!")
    );


  }

  /**
   * Verify that JWKS endpoint is only called once the first time a JWT is verified
   */
  @Test
  public void verifyJwkIsCached() {
    String accountId = "bill";
    ImmutableJwtAuthSettings authSettings = JwtAuthSettings.builder()
      .tokenSubject(accountId)
      .tokenAudience("foo")
      .tokenIssuer(HttpUrl.parse(wireMockRule.baseUrl()))
      .build();
    createAccount(AccountId.of(accountId), customSettingsJwtRs256(
      authSettings
    ));
    for (int i = 0; i < 2; i++) {
      String jwt = jwtServer.createJwt(authSettings, Instant.now().plusSeconds(30));
      final IlpOverHttpLink ilpOverHttpLink = ilpOverHttpLink(AccountId.of(accountId), authSettings, jwt);
      ilpOverHttpLink.testConnection();
    }

    verify(exactly(1), getRequestedFor(urlEqualTo(WELL_KNOWN_JWKS_JSON)));
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Construct a new HTTP HTTP Client for the `bob` account
   */
  private IlpOverHttpLink ilpOverHttpLink(AccountId accountId, JwtAuthSettings authSettings, String jwt) {
    IlpOverHttpLink link = new IlpOverHttpLink(
      () -> InterledgerAddress.of("test." + accountId.value()),
      createAccountIlpUrl(template.getRootUri(), accountId),
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      () -> jwt
    );
    link.setLinkId(LinkId.of(accountId.value()));
    return link;
  }

}
