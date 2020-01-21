package org.interledger.connector.server.ilpoverhttp;

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
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.crypto.Decryptor;
import org.interledger.link.LinkId;
import org.interledger.link.LoopbackLink;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.JwtHs256BearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
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
 * using the `JWT_HS_256` profile defined in the Interledger HTTP Authentication recommendations RFC.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
public class JwtHs256IlpOverHttpEndpointTest extends AbstractEndpointTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
  public void setUp() {

    ////////////////
    // Add the Alice Account to the Connector.
    ////////////////

    if (!adminApiTestClient.findAccount(ALICE).isPresent()) {
      final Map<String, Object> customSettings = Maps.newHashMap();
      customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, "JWT_HS_256 ");
      customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, "JWT_HS_256 ");
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, CONNIE);
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
      customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://alice.example.com");

      final AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of("alice"))
        .description("Loopback account for Alice using a simple shared-secret")
        .accountRelationship(AccountRelationship.PEER)
        .linkType(LoopbackLink.LINK_TYPE)
        .customSettings(customSettings)
        .assetScale(2)
        .assetCode("XRP")
        .build();
      adminApiTestClient.createAccount(accountSettings);
    }

    if (!externalRoutingService.findBestNexHop(InterledgerAddress.of("test.connie.alice")).isPresent()) {
      externalRoutingService.createStaticRoute(
        StaticRoute.builder()
          .nextHopAccountId(AccountId.of("alice"))
          .routePrefix(InterledgerAddressPrefix.of("test.connie.alice"))
          .build()
      );
    }

    if (!externalRoutingService.findBestNexHop(InterledgerAddress.of("test.connie.bob")).isPresent()) {
      externalRoutingService.createStaticRoute(
        StaticRoute.builder()
          .nextHopAccountId(AccountId.of("bob"))
          .routePrefix(InterledgerAddressPrefix.of("test.connie.bob"))
          .build()
      );
    }
  }

  /**
   * Validate that Bob can send a packet to `alice` and get a fulfillment back. Since `alice` is operating a loopback
   * account, we always expect the {@link LoopbackLink#LOOPBACK_FULFILLMENT} to be returned, so we must make sure that
   * the prepare packets use the correct corresponding condition derived from that fulfillment.
   */
  @Test
  public void bobPaysAliceUsingIlpOverHttp() {
    createAccount(AccountId.of(BOB), customSettingsJwtHs256(ENCRYPTED_SHH, BOB));
    final IlpOverHttpLink ilpOverHttpLink = ilpOverHttpLink(AccountId.of(BOB), BOB);

    ilpOverHttpLink.sendPacket(
      InterledgerPreparePacket.builder()
        .destination(InterledgerAddress.of("test.connie.alice"))
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
   * Validate the "test connection" method in the IL-DCP requester created with an encrypted secret.
   */
  @Test
  public void ildcpTestConnectionWithEncryptedSecret() {
    AccountId accountId = AccountId.of("mark");
    createAccount(accountId, customSettingsJwtHs256(ENCRYPTED_SHH, accountId.value()));
    IlpOverHttpLink link = ilpOverHttpLink(accountId, accountId.value());
    assertLink(link);
  }

  /**
   * Validate the "test connection" method in the IL-DCP requestor created with an base64 encoded secret.
   */
  @Test
  public void ildcpTestConnectionWithPlainTextSecret() {
    AccountId accountId = AccountId.of("lisa");
    createAccount(accountId, customSettingsJwtHs256("shh", accountId.value()));
    IlpOverHttpLink link = ilpOverHttpLink(accountId, accountId.value());
    assertLink(link);
  }

  /**
   * Validate the request fails if the provided JWT subject does not match the account settings incoming.jwt.token_subject
   */
  @Test
  public void failsIfSubjectDoesNotMatch() {
    AccountId accountId = AccountId.of("bart");
    createAccount(accountId, customSettingsJwtHs256("shh", accountId.value()));
    IlpOverHttpLink link = ilpOverHttpLink(accountId, "hacker");
    expectedException.expect(LinkException.class);
    assertLink(link);
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Construct a new HTTP HTTP Client for the `bob` account
   */
  private IlpOverHttpLink ilpOverHttpLink(AccountId accountId, String jwtSubject) {
    final OutgoingLinkSettings outgoingLinkSettings = OutgoingLinkSettings.builder()
      .authType(AuthType.JWT_HS_256)
      .jwtAuthSettings(
        JwtAuthSettings.builder()
          .tokenSubject(jwtSubject)
          .encryptedTokenSharedSecret(ENCRYPTED_SHH)
          .build()
      )
      .url(createAccountIlpUrl(template.getRootUri(), accountId))
      .build();

    IlpOverHttpLink link = new IlpOverHttpLink(
      () -> InterledgerAddress.of("test." + accountId.value()),
      createAccountIlpUrl(template.getRootUri(), accountId),
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      new JwtHs256BearerTokenSupplier(
        "shh"::getBytes,
        outgoingLinkSettings
      )
    );
    link.setLinkId(LinkId.of(accountId.value()));
    return link;
  }

}
