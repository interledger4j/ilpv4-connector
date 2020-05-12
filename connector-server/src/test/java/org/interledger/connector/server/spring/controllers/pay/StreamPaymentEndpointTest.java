package org.interledger.connector.server.spring.controllers.pay;


import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.pay.StreamPaymentEndpointTest.TEST_DOT_BOB;
import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.LoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.receiver.ServerSecretSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

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
@ActiveProfiles( {"test-postgres"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
@TestPropertySource(
  properties = {
    ConfigConstants.ENABLED_FEATURES + "." + ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED + "=true",
    "interledger.connector.nodeIlpAddress=" + TEST_DOT_BOB
  }
)
@AutoConfigureEmbeddedDatabase
public class StreamPaymentEndpointTest extends AbstractEndpointTest {

  public static final String DAVID = "david";
  protected static final String TEST_DOT_BOB = "test.bob";
  private static final String LOCAL_SENDER = "local_sender";
  private static final String LOCAL_RECEIVER = "local_receiver";
  private static final String SUB_ACCOUNT_RECEIVER = "sub_account_receiver";
  private static final String FULFILLABLE_FORWARDING_ACCOUNT = "fulfillable_forwarding_account";
  private static final String USD = "USD";
  private static final String XRP = "XRP";
  private static final String SHH = "shh";
  private static final String ACCOUNTS = "accounts";
  private static final String ILP = "ilp";
  private static final InterledgerAddress CONNECTOR_OPERATOR_ADDRESS = InterledgerAddress.of(TEST_DOT_BOB);

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

  @Value("${" + ConfigConstants.SPSP__URL_PATH + ":}")
  private String spspUrlPath;

  private HttpUrl testnetHost;

  @Before
  public void setUp() {
    this.testnetHost = HttpUrl.parse("http://localhost:" + localServerPort);
    when(serverSecretSupplier.get()).thenReturn(new byte[32]);

    adminApiTestClient.findAccount(LOCAL_SENDER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings();
      customSettings.put(
        "ilpOverHttp.outgoing.url", "https://example.com/foo"
      );
      final AccountSettings senderAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(LOCAL_SENDER))
        .description("The sender of SPSP payments")
        .accountRelationship(AccountRelationship.CHILD)
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .customSettings(customSettings)
        .maximumPacketAmount(UnsignedLong.valueOf(10000))
        .assetCode(USD)
        .assetScale(3)
        .build();
      return adminApiTestClient.createAccount(senderAccountSettings);
    });

    // Send to this account to process an SPSP payment.
    adminApiTestClient.findAccount(LOCAL_RECEIVER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings();
      customSettings.put(
        "ilpOverHttp.outgoing.url", "https://example.com/foo"
      );
      final AccountSettings receiverAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(LOCAL_RECEIVER))
        .description("The receiver of SPSP payments")
        .accountRelationship(AccountRelationship.PEER)
        .maximumPacketAmount(UnsignedLong.valueOf(100000))
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .customSettings(customSettings)
        .assetCode(XRP)
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
  public void sendDurablePayment() throws ExecutionException, InterruptedException {
    final UnsignedLong amountToSend = UnsignedLong.valueOf(100000L);

    PaymentRequest request = PaymentRequest.builder()
      .amount(amountToSend)
      .destinationPaymentPointer(localPaymentPointer(LOCAL_RECEIVER).toString())
      .build();

    PaymentResponse response = userClient.sendPayment(bearer(SHH), AccountId.of(LOCAL_SENDER), request);
    assertThat(response.amountSent()).isEqualTo(request.amount());
    assertThat(response.expectedAmount()).isNotEmpty()
      .hasValue(amountToSend.bigIntegerValue().negate());
    assertThat(response.successfulPayment()).isTrue();


    ListPaymentsResponse senderPayments = userClient.listPayments(bearer(SHH), AccountId.of(LOCAL_SENDER));

    assertThat(senderPayments.payments()).contains(StreamPayment.builder().from(response).build());

    ListPaymentsResponse receiverPayments = userClient.listPayments(bearer(SHH), AccountId.of(LOCAL_RECEIVER));
    assertThat(receiverPayments.payments()).isNotEmpty()
      .extracting(StreamPayment::deliveredAmount)
      .contains(response.deliveredAmount());

  }

  private PaymentPointer localPaymentPointer(String accountId) {
    return PaymentPointer.of("$localhost:" + localServerPort + "/" + spspUrlPath + "/" + accountId);
  }

//  /**
//   * This test proves that when local SPSP fulfillment is enabled, packets that are meant for a local account (simulated
//   * to be connected to the Connector) are fulfilled properly and not routed to the SPSP receiver.
//   */
//  @Test
//  public void sendLocalAccountPayment() {
//    final Link link = getSenderLink();
//
//    for (int i = 0; i < 100; i++) {
//      link.sendPacket(InterledgerPreparePacket.builder()
//        .amount(UnsignedLong.ONE)
//        .expiresAt(Instant.now().plusSeconds(30))
//        .destination(CONNECTOR_OPERATOR_ADDRESS.with("accounts").with(SUB_ACCOUNT_RECEIVER))
//        .executionCondition(LoopbackLink.LOOPBACK_FULFILLMENT.getCondition())
//        .build()).handle(
//        interledgerFulfillPacket -> {
//          assertThat(interledgerFulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT);
//        },
//        interledgerRejectPacket -> {
//          fail("Should have fulfilled, but did not");
//        }
//      );
//    }
//  }
//
//  /**
//   * This test proves that when local SPSP fulfillment is enabled, packets that are meant to be forwarded are fulfilled
//   * properly and not routed to the SPSP receiver nor to a sub-account.
//   */
//  @Test
//  public void sendExternalPaymentToWorkingForwardingAccount() {
//    final Link link = getSenderLink();
//
//    for (int i = 0; i < 100; i++) {
//      link.sendPacket(InterledgerPreparePacket.builder()
//        .amount(UnsignedLong.ONE)
//        .expiresAt(Instant.now().plusSeconds(30))
//        .destination(CONNECTOR_OPERATOR_ADDRESS.with(DAVID))
//        .executionCondition(LoopbackLink.LOOPBACK_FULFILLMENT.getCondition())
//        .build()).handle(
//        interledgerFulfillPacket -> {
//          assertThat(interledgerFulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT);
//        },
//        interledgerRejectPacket -> {
//          fail("Should have fulfilled, but did not");
//        }
//      );
//    }
//  }
//
//  /**
//   * This test proves that when local SPSP fulfillment is enabled, packets that are meant to be forwarded are rejected
//   * properly if there is no route to the remote destination address.
//   */
//  @Test
//  public void sendExternalPaymentToUnroutableForwardingAccount() {
//    final Link link = getSenderLink();
//
//    for (int i = 0; i < 100; i++) {
//      link.sendPacket(InterledgerPreparePacket.builder()
//        .amount(UnsignedLong.ONE)
//        .expiresAt(Instant.now().plusSeconds(30))
//        .destination(InterledgerAddress.of("test.foo.baz")) // not routable
//        .executionCondition(LoopbackLink.LOOPBACK_FULFILLMENT.getCondition())
//        .build()).handle(
//        interledgerFulfillPacket -> {
//          fail("Should have rejected, but did not");
//        },
//        interledgerRejectPacket -> {
//          assertThat(interledgerRejectPacket.getCode()).isEqualTo(InterledgerErrorCode.F02_UNREACHABLE);
//          assertThat(interledgerRejectPacket.getTriggeredBy().get()).isEqualTo(CONNECTOR_OPERATOR_ADDRESS);
//          assertThat(interledgerRejectPacket.getMessage()).isEqualTo("Destination address is unreachable");
//        }
//      );
//    }
//  }

  private Map<String, Object> constructAccountCustomSettings() {
    Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put("ilpOverHttp.incoming.auth_type", AuthType.SIMPLE.name());
    customSettings.put("ilpOverHttp.incoming.simple.auth_token", SHH);
    customSettings.put("ilpOverHttp.outgoing.auth_type", AuthType.SIMPLE.name());
    customSettings.put("ilpOverHttp.outgoing.simple.auth_token", SHH);
    return customSettings;
  }
}
