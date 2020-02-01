package org.interledger.connector.server.spsp;


import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;
import static org.interledger.connector.server.spsp.LocalSpspPaymentFulfillmentEndpointTest.TEST_DOT_CONNIE;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.link.Link;
import org.interledger.link.LoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StreamConnectionGenerator;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Tests the Connector's ability to locally fulifill SPSP/STREAM packets, in addition to other packet-types while
 * locally-fulfilled SPSP functionality is enabled.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
@TestPropertySource(
  properties = {
    ConfigConstants.ENABLED_FEATURES + "." + ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED + "=true",
    "interledger.connector.nodeIlpAddress=" + TEST_DOT_CONNIE
  }
)
public class LocalSpspPaymentFulfillmentEndpointTest extends AbstractEndpointTest {

  public static final String DAVID = "david";
  static final String TEST_DOT_CONNIE = "test.connie";
  private static final String SPSP_SENDER = "spsp_sender";
  private static final String SPSP_RECEIVER = "spsp_receiver";
  private static final String SUB_ACCOUNT_RECEIVER = "sub_account_receiver";
  private static final String FULFILLABLE_FORWARDING_ACCOUNT = "fulfillable_forwarding_account";
  private static final String USD = "USD";
  private static final String SHH = "shh";
  private static final String ACCOUNTS = "accounts";
  private static final String ILP = "ilp";
  private static final InterledgerAddress CONNECTOR_OPERATOR_ADDRESS = InterledgerAddress.of(TEST_DOT_CONNIE);
  private final StreamConnectionGenerator streamConnectionGenerator = new SpspStreamConnectionGenerator();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @MockBean
  private ServerSecretSupplier serverSecretSupplier;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient okHttpClient;

  @Autowired
  private ObjectMapper objectMapper;

  private HttpUrl testnetHost;

  @Before
  public void setUp() {
    this.testnetHost = HttpUrl.parse("http://localhost:" + localServerPort);
    when(serverSecretSupplier.get()).thenReturn(new byte[32]);

    adminApiTestClient.findAccount(SPSP_SENDER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings();
      customSettings.put(
        "ilpOverHttp.outgoing.url", "https://example.com/foo"
      );
      final AccountSettings senderAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(SPSP_SENDER))
        .description("The sender of SPSP payments")
        .accountRelationship(AccountRelationship.CHILD)
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .customSettings(customSettings)
        .maximumPacketAmount(UnsignedLong.valueOf(1000))
        .assetCode(USD)
        .assetScale(3)
        .build();
      return adminApiTestClient.createAccount(senderAccountSettings);
    });

    // Send to this account to process an SPSP payment.
    adminApiTestClient.findAccount(SPSP_RECEIVER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings();
      customSettings.put(
        "ilpOverHttp.outgoing.url", "https://example.com/foo"
      );
      final AccountSettings receiverAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(SPSP_RECEIVER))
        .description("The receiver of SPSP payments")
        .accountRelationship(AccountRelationship.PEER)
        .maximumPacketAmount(UnsignedLong.valueOf(1000))
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .customSettings(customSettings)
        .assetCode(USD)
        .assetScale(3)
        .build();

      return adminApiTestClient.createAccount(receiverAccountSettings);
    });

    // Send to this account to simulate forwarding of packets to a child sub-account.
    adminApiTestClient.findAccount(SUB_ACCOUNT_RECEIVER).orElseGet(() -> {
      final AccountSettings senderAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(SUB_ACCOUNT_RECEIVER))
        .description("The receiver of sub-account payments")
        .accountRelationship(AccountRelationship.CHILD)
        .linkType(LoopbackLink.LINK_TYPE)
        .maximumPacketAmount(UnsignedLong.valueOf(1000))
        .assetCode(USD)
        .assetScale(3)
        .build();
      final AccountSettings accountSettings = adminApiTestClient.createAccount(senderAccountSettings);

      // Add this static route so that packets destined for `test.connie.sub_account_receiver` will be fulfilled via
      // the loopback functionality. This demonstrates that the Connector can natively route to `test.connie.accounts
      // .sub_accounts_receiver` without any routing table entry, but can also route to statically-defined routes, too.
      adminApiTestClient.createStaticRoute(
        CONNECTOR_OPERATOR_ADDRESS.with(DAVID).getValue(),
        StaticRoute.builder()
          .nextHopAccountId(accountSettings.accountId())
          .routePrefix(InterledgerAddressPrefix.of(CONNECTOR_OPERATOR_ADDRESS.with(DAVID).getValue()))
          .build()
      );
      return accountSettings;
    });

    // Send to this account to simulate failed forwarding of packets when `accounts` and `spsp` are allowed.
    adminApiTestClient.findAccount(FULFILLABLE_FORWARDING_ACCOUNT)
      .orElseGet(() -> {
        final AccountSettings senderAccountSettings = AccountSettings.builder()
          .accountId(AccountId.of(FULFILLABLE_FORWARDING_ACCOUNT))
          .description("An account that forwards without a route in the routing table.")
          .accountRelationship(AccountRelationship.PEER)
          .linkType(LoopbackLink.LINK_TYPE)
          .maximumPacketAmount(UnsignedLong.valueOf(1000))
          .assetCode(USD)
          .assetScale(3)
          .build();
        final AccountSettings accountSettings = adminApiTestClient.createAccount(senderAccountSettings);
        adminApiTestClient.createStaticRoute("test.foo.bar", StaticRoute.builder()
          .nextHopAccountId(accountSettings.accountId())
          .routePrefix(InterledgerAddressPrefix.of("test.foo.bar"))
          .build());
        return accountSettings;
      });
  }

  /**
   * This test proves that when local SPSP fulfillment is enabled, the packets are routed to the SPSP receiver link and
   * fulfilled properly as part of a STREAM payment.
   */
  @Test
  public void sendSpspPayment() throws ExecutionException, InterruptedException {
    final Link link = getSenderLink();

    final StreamConnectionDetails connectionDetails = streamConnectionGenerator.generateConnectionDetails(
      serverSecretSupplier, InterledgerAddress.of("test.connie.spsp." + SPSP_RECEIVER)
    );

    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);

    final UnsignedLong amountToSend = UnsignedLong.valueOf(1000000L); // $1000.00
    final SendMoneyRequest sendMoneyRequest = SendMoneyRequest.builder()
      .sourceAddress(CONNECTOR_OPERATOR_ADDRESS.with(SPSP_SENDER))
      .amount(amountToSend)
      .denomination(Denomination.builder().assetScale((short) 3).assetCode(USD).build())
      .destinationAddress(connectionDetails.destinationAddress())
      .sharedSecret(connectionDetails.sharedSecret())
      .paymentTracker(new FixedSenderAmountPaymentTracker(amountToSend))
      .build();
    final SendMoneyResult sendMoneyResult = simpleStreamSender
      .sendMoney(sendMoneyRequest).get();

    logger.info("STREAM Payment Complete: {}", sendMoneyResult);
    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(amountToSend);
    assertThat(sendMoneyResult.amountLeftToSend()).isEqualTo(UnsignedLong.ZERO);
    assertThat(sendMoneyResult.amountSent()).isEqualTo(amountToSend);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(amountToSend);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1000);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.successfulPayment()).isTrue();
    assertThat(sendMoneyResult.totalPackets()).isEqualTo(1001);
  }

  /**
   * This test proves that when local SPSP fulfillment is enabled, packets that are meant for a local account (simulated
   * to be connected to the Connector) are fulfilled properly and not routed to the SPSP receiver.
   */
  @Test
  public void sendLocalAccountPayment() {
    final Link link = getSenderLink();

    for (int i = 0; i < 100; i++) {
      link.sendPacket(InterledgerPreparePacket.builder()
        .amount(UnsignedLong.ONE)
        .expiresAt(Instant.now().plusSeconds(30))
        .destination(CONNECTOR_OPERATOR_ADDRESS.with("accounts").with(SUB_ACCOUNT_RECEIVER))
        .executionCondition(LoopbackLink.LOOPBACK_FULFILLMENT.getCondition())
        .build()).handle(
        interledgerFulfillPacket -> {
          assertThat(interledgerFulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT);
        },
        interledgerRejectPacket -> {
          fail("Should have fulfilled, but did not");
        }
      );
    }
  }

  /**
   * This test proves that when local SPSP fulfillment is enabled, packets that are meant to be forwarded are fulfilled
   * properly and not routed to the SPSP receiver nor to a sub-account.
   */
  @Test
  public void sendExternalPaymentToWorkingForwardingAccount() {
    final Link link = getSenderLink();

    for (int i = 0; i < 100; i++) {
      link.sendPacket(InterledgerPreparePacket.builder()
        .amount(UnsignedLong.ONE)
        .expiresAt(Instant.now().plusSeconds(30))
        .destination(CONNECTOR_OPERATOR_ADDRESS.with(DAVID))
        .executionCondition(LoopbackLink.LOOPBACK_FULFILLMENT.getCondition())
        .build()).handle(
        interledgerFulfillPacket -> {
          assertThat(interledgerFulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT);
        },
        interledgerRejectPacket -> {
          fail("Should have fulfilled, but did not");
        }
      );
    }
  }

  /**
   * This test proves that when local SPSP fulfillment is enabled, packets that are meant to be forwarded are rejected
   * properly if there is no route to the remote destination address.
   */
  @Test
  public void sendExternalPaymentToUnroutableForwardingAccount() {
    final Link link = getSenderLink();

    for (int i = 0; i < 100; i++) {
      link.sendPacket(InterledgerPreparePacket.builder()
        .amount(UnsignedLong.ONE)
        .expiresAt(Instant.now().plusSeconds(30))
        .destination(InterledgerAddress.of("test.foo.baz")) // not routable
        .executionCondition(LoopbackLink.LOOPBACK_FULFILLMENT.getCondition())
        .build()).handle(
        interledgerFulfillPacket -> {
          fail("Should have rejected, but did not");
        },
        interledgerRejectPacket -> {
          assertThat(interledgerRejectPacket.getCode()).isEqualTo(InterledgerErrorCode.F02_UNREACHABLE);
          assertThat(interledgerRejectPacket.getTriggeredBy().get()).isEqualTo(CONNECTOR_OPERATOR_ADDRESS);
          assertThat(interledgerRejectPacket.getMessage()).isEqualTo("Destination address is unreachable");
        }
      );
    }
  }

  /**
   * Construct and return a new {@link Link} that provides native ILP access to a remote caller identified as {@link
   * #SPSP_SENDER}.
   *
   * @return A {@link Link}.
   */
  private IlpOverHttpLink getSenderLink() {
    return new IlpOverHttpLink(
      () -> CONNECTOR_OPERATOR_ADDRESS.with(SPSP_SENDER),
      testnetHost.newBuilder().addPathSegment(ACCOUNTS).addPathSegment(SPSP_SENDER).addPathSegment(ILP).build(),
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(SHH)
    );
  }

  private Map<String, Object> constructAccountCustomSettings() {
    Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put("ilpOverHttp.incoming.auth_type", AuthType.SIMPLE.name());
    customSettings.put("ilpOverHttp.incoming.simple.auth_token", SHH);
    customSettings.put("ilpOverHttp.outgoing.auth_type", AuthType.SIMPLE.name());
    customSettings.put("ilpOverHttp.outgoing.simple.auth_token", SHH);
    return customSettings;
  }
}
