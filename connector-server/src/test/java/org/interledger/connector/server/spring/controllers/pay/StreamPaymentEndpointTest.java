package org.interledger.connector.server.spring.controllers.pay;


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.controllers.pay.StreamPaymentEndpointTest.TEST_DOT_BOB;
import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.payments.ListStreamPaymentsResponse;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.connector.server.spsp.LocalSpspClientTestConfig;
import org.interledger.connector.server.spsp.SimpleSpspTestRecevier;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.Denomination;
import org.interledger.stream.ImmutableDenomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Tests the connectors ability to send durable STREAM payments via the /accounts/{id}/payments API and fetch
 * payment history.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class, LocalSpspClientTestConfig.class, StreamPaymentEndpointTest.TestConfig.class}
)
@ActiveProfiles( {"test","test-postgres"}) // Uses the `application-test.properties` file in the `src/test/resources` folder
@TestPropertySource(
  properties = {
    ConfigConstants.ENABLED_FEATURES + "." + ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED + "=true",
    "interledger.connector.nodeIlpAddress=" + TEST_DOT_BOB
  }
)
@AutoConfigureEmbeddedDatabase
public class StreamPaymentEndpointTest extends AbstractEndpointTest {

  protected static final String TEST_DOT_BOB = "test.bob";
  private static final String SENDER = "local_sender";
  private static final String LOCAL_RECEIVER = "local_receiver";
  private static final String REMOTE_RECEIVER = "remote_receiver";
  private static final String PEER_FORWARDING_ACCOUNT = "peer_forwarding";
  private static final InterledgerAddress REMOTE_RECEIVER_ADDRESS = InterledgerAddress.of("test.remote-peer")
    .with(REMOTE_RECEIVER);
  private static final InterledgerAddressPrefix REMOTE_PEER = REMOTE_RECEIVER_ADDRESS.getPrefix();

  private static final String USD = "USD";
  private static final String XRP = "XRP";
  private static final String SHH = "shh";
  public static final ImmutableDenomination DENOMINATION_XRP = Denomination.builder().assetCode("XRP").assetScale((short) 9).build();

  @MockBean
  private ServerSecretSupplier serverSecretSupplier;

  @Autowired
  private StatelessStreamReceiver streamReceiver;

  @Autowired
  private CodecContext ilpCodecContext;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  private OkHttpClient okHttpClient;

  @Autowired
  private SpspClient localSpspClient;

  // for testing a remote receiver
  @Rule
  public WireMockRule remoteSpspReceiverWireMockRule = new WireMockRule(options()
    .dynamicPort()
    .extensions(SimpleSpspTestRecevier.transformer(
      () -> streamReceiver,
      () -> ilpCodecContext,
      () -> objectMapper,
      REMOTE_RECEIVER_ADDRESS,
      DENOMINATION_XRP
    ))
  );

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Value("${" + ConfigConstants.SPSP__URL_PATH + ":}")
  private String spspUrlPath;

  private HttpUrl testnetHost;

  @Before
  public void setUp() {
    HttpUrl remoteReceiverUrl = SimpleSpspTestRecevier.stubReceiver(remoteSpspReceiverWireMockRule);
    this.testnetHost = HttpUrl.parse("http://localhost:" + localServerPort);
    when(serverSecretSupplier.get()).thenReturn(new byte[32]);

    adminApiTestClient.findAccount(SENDER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings(SENDER);
      final AccountSettings senderAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(SENDER))
        .description("The sender of SPSP payments")
        .accountRelationship(AccountRelationship.CHILD)
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .isSendRoutes(false)
        .customSettings(customSettings)
        .maximumPacketAmount(UnsignedLong.valueOf(10000))
        .assetCode(USD)
        .assetScale(3)
        .build();
      return adminApiTestClient.createAccount(senderAccountSettings);
    });

    // Send to this account to process an SPSP payment.
    adminApiTestClient.findAccount(LOCAL_RECEIVER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings(LOCAL_RECEIVER);
      final AccountSettings receiverAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(LOCAL_RECEIVER))
        .description("The receiver of SPSP payments")
        .accountRelationship(AccountRelationship.PEER)
        .maximumPacketAmount(UnsignedLong.valueOf(100000))
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .isSendRoutes(false)
        .customSettings(customSettings)
        .assetCode(XRP)
        .assetScale(3)
        .build();

      return adminApiTestClient.createAccount(receiverAccountSettings);
    });

    // account for forwarding packets to the remote receiver running in WireMock
    // must deleted because the outbound settings to the WireMock remote receiver change on every run
    adminApiTestClient.findAccount(PEER_FORWARDING_ACCOUNT).map(account -> {
      adminApiTestClient.deleteAccount(account.accountId().value());
      return newForwardingAccount(remoteReceiverUrl);
    }).orElseGet(() -> {
      final AccountSettings accountSettings = newForwardingAccount(remoteReceiverUrl);
      adminApiTestClient.createStaticRoute(REMOTE_PEER.getValue(), StaticRoute.builder()
        .nextHopAccountId(accountSettings.accountId())
        .routePrefix(REMOTE_PEER)
        .build());
      return accountSettings;
    });
  }

  private AccountSettings newForwardingAccount(HttpUrl remoteReceiverUrl) {
    final Map<String, Object> customSettings = constructAccountCustomSettings(PEER_FORWARDING_ACCOUNT);
    customSettings.put("ilpOverHttp.outgoing.url", remoteReceiverUrl.toString());
    final AccountSettings senderAccountSettings = AccountSettings.builder()
      .accountId(AccountId.of(PEER_FORWARDING_ACCOUNT))
      .description("An account that forwards without a route in the routing table.")
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .isSendRoutes(false)
      .maximumPacketAmount(UnsignedLong.valueOf(1000))
      .assetCode(USD)
      .assetScale(3)
      .customSettings(customSettings)
      .build();
    return adminApiTestClient.createAccount(senderAccountSettings);
  }

  /**
   * This test proves that when local SPSP fulfillment is enabled, the packets are routed to the SPSP receiver link and
   * fulfilled properly as part of a STREAM payment.
   */
  @Test
  public void sendDurablePaymentWithLocalSendAndLocalFulfill() {
    final UnsignedLong amountToSend = UnsignedLong.valueOf(19389);

    String correlationId = "sendDurablePaymentWithLocalSendAndLocalFulfill";
    LocalSendPaymentRequest request = LocalSendPaymentRequest.builder()
      .amount(amountToSend)
      .destinationPaymentPointer(localPaymentPointer(LOCAL_RECEIVER).toString())
      .correlationId(correlationId)
      .build();

    LocalSendPaymentResponse response = userClient.sendPayment(bearer(SHH), AccountId.of(SENDER), request);
    assertThat(response.amountSent()).isEqualTo(request.amount());
    assertThat(response.expectedAmount()).isNotEmpty()
      .hasValue(amountToSend.bigIntegerValue().negate());
    assertThat(response.successfulPayment()).isTrue();
    assertThat(response.correlationId()).hasValue(correlationId);

    ListStreamPaymentsResponse senderPayments = userClient.listPayments(
      bearer(SHH),
      AccountId.of(SENDER),
      ImmutableMap.of("page", 0, "correlationId", correlationId)
    );
    assertThat(senderPayments.pageNumber()).isEqualTo(0);
    assertThat(senderPayments.pageSize()).isEqualTo(100);
    assertThat(filterByDestinationAddress(senderPayments.payments(), response.destinationAddress()))
      .contains(StreamPayment.builder().from(response).build());

    // verify no result on the next page
    assertThat(userClient.listPayments(bearer(SHH), AccountId.of(SENDER), ImmutableMap.of("page", 1)).payments())
      .isEmpty();

    ListStreamPaymentsResponse receiverPayments = userClient.listPayments(bearer(SHH), AccountId.of(LOCAL_RECEIVER));
    Optional<StreamPayment> receiverPayment =
      filterByDestinationAddress(receiverPayments.payments(), response.destinationAddress());

    assertThat(receiverPayment).isNotEmpty();
    assertThat(receiverPayment.get().deliveredAmount()).isEqualTo(response.deliveredAmount());
    assertThat(receiverPayment.get().expectedAmount()).isEqualTo(Optional.empty());
  }

  @Test
  public void sendDurablePaymentWithLocalSendAndRemoteFulfill() {
    final UnsignedLong amountToSend = UnsignedLong.valueOf(67388);

    LocalSendPaymentRequest request = LocalSendPaymentRequest.builder()
      .amount(amountToSend)
      .destinationPaymentPointer(remotePaymentPointer().toString())
      .build();

    LocalSendPaymentResponse response = userClient.sendPayment(bearer(SHH), AccountId.of(SENDER), request);
    assertThat(response.amountSent()).isEqualTo(request.amount());
    assertThat(response.expectedAmount()).isNotEmpty()
      .hasValue(amountToSend.bigIntegerValue().negate());
    assertThat(response.successfulPayment()).isTrue();

    ListStreamPaymentsResponse senderPayments = userClient.listPayments(bearer(SHH), AccountId.of(SENDER));
    assertThat(filterByDestinationAddress(senderPayments.payments(), response.destinationAddress()))
      .contains(StreamPayment.builder().from(response).build());

    ListStreamPaymentsResponse receiverPayments = userClient.listPayments(bearer(SHH), AccountId.of(PEER_FORWARDING_ACCOUNT));
    Optional<StreamPayment> receiverPayment =
      filterByDestinationAddress(receiverPayments.payments(), response.destinationAddress());

    assertThat(receiverPayment).isNotEmpty();
    assertThat(receiverPayment.get().deliveredAmount()).isEqualTo(response.deliveredAmount());
    assertThat(receiverPayment.get().expectedAmount()).isEqualTo(Optional.empty());
  }

  @Test
  public void sendDurablePaymentWithRemoteSendAndLocalFulfill() throws ExecutionException, InterruptedException {
    final UnsignedLong amountToSend = UnsignedLong.valueOf(34663);

    StreamConnectionDetails connectionDetails =
      localSpspClient.getStreamConnectionDetails(localPaymentPointer(LOCAL_RECEIVER));

    SimpleStreamSender simpleStreamSender =
      new SimpleStreamSender(linkToServer(AccountId.of(SENDER), SHH, okHttpClient, objectMapper));

    final SendMoneyRequest sendMoneyRequest = SendMoneyRequest.builder()
      .amount(amountToSend)
      .denomination(DENOMINATION_XRP)
      .destinationAddress(connectionDetails.destinationAddress())
      .sharedSecret(connectionDetails.sharedSecret())
      .paymentTracker(new FixedSenderAmountPaymentTracker(amountToSend))
      .build();
    final SendMoneyResult sendMoneyResult = simpleStreamSender.sendMoney(sendMoneyRequest).get();

    ListStreamPaymentsResponse senderPayments = userClient.listPayments(bearer(SHH), AccountId.of(SENDER));
    Optional<StreamPayment> senderPayment =
      filterByDestinationAddress(senderPayments.payments(), sendMoneyResult.destinationAddress());

    assertThat(senderPayment).isNotEmpty();
    assertThat(senderPayment.get().amount()).isEqualTo(sendMoneyResult.amountSent().bigIntegerValue().negate());
    assertThat(senderPayment.get().deliveredAmount()).isEqualTo(sendMoneyResult.amountDelivered());
    assertThat(senderPayment.get().expectedAmount()).isEqualTo(Optional.empty());

    ListStreamPaymentsResponse receiverPayments = userClient.listPayments(bearer(SHH), AccountId.of(LOCAL_RECEIVER));
    Optional<StreamPayment> receiverPayment =
      filterByDestinationAddress(receiverPayments.payments(), sendMoneyResult.destinationAddress());

    assertThat(receiverPayment).isNotEmpty();
    assertThat(receiverPayment.get().deliveredAmount()).isEqualTo(sendMoneyResult.amountDelivered());
    assertThat(receiverPayment.get().expectedAmount()).isEqualTo(Optional.empty());
  }

  @Test
  public void sendDurablePaymentWithRemoteSendAndRemoteFulfill() throws ExecutionException, InterruptedException {
    final UnsignedLong amountToSend = UnsignedLong.valueOf(38712);

    StreamConnectionDetails connectionDetails = localSpspClient.getStreamConnectionDetails(remotePaymentPointer());

    SimpleStreamSender simpleStreamSender =
      new SimpleStreamSender(linkToServer(AccountId.of(SENDER), SHH, okHttpClient, objectMapper));

    final SendMoneyRequest sendMoneyRequest = SendMoneyRequest.builder()
      .amount(amountToSend)
      .denomination(DENOMINATION_XRP)
      .destinationAddress(connectionDetails.destinationAddress())
      .sharedSecret(connectionDetails.sharedSecret())
      .paymentTracker(new FixedSenderAmountPaymentTracker(amountToSend))
      .build();
    final SendMoneyResult sendMoneyResult = simpleStreamSender.sendMoney(sendMoneyRequest).get();

    ListStreamPaymentsResponse senderPayments = userClient.listPayments(bearer(SHH), AccountId.of(SENDER));
    Optional<StreamPayment> senderPayment =
      filterByDestinationAddress(senderPayments.payments(), sendMoneyResult.destinationAddress());

    assertThat(senderPayment).isNotEmpty();
    assertThat(senderPayment.get().amount()).isEqualTo(sendMoneyResult.amountSent().bigIntegerValue().negate());
    assertThat(senderPayment.get().deliveredAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(senderPayment.get().expectedAmount()).isEqualTo(Optional.empty());

    ListStreamPaymentsResponse receiverPayments = userClient.listPayments(bearer(SHH), AccountId.of(PEER_FORWARDING_ACCOUNT));
    Optional<StreamPayment> receiverPayment =
      filterByDestinationAddress(receiverPayments.payments(), sendMoneyResult.destinationAddress());

    assertThat(receiverPayment).isNotEmpty();
    assertThat(receiverPayment.get().deliveredAmount()).isEqualTo(sendMoneyResult.amountDelivered());
    assertThat(receiverPayment.get().expectedAmount()).isEqualTo(Optional.empty());
  }

  private PaymentPointer localPaymentPointer(String accountId) {
    return PaymentPointer.of("$localhost:" + localServerPort + "/" + spspUrlPath + "/" + accountId);
  }

  private PaymentPointer remotePaymentPointer() {
    return PaymentPointer.of("$localhost:" + remoteSpspReceiverWireMockRule.port());
  }

  private Map<String, Object> constructAccountCustomSettings(String accountId) {
    Map<String, Object> customSettings = Maps.newHashMap();
    customSettings.put("ilpOverHttp.incoming.auth_type", AuthType.SIMPLE.name());
    customSettings.put("ilpOverHttp.incoming.simple.auth_token", SHH);
    customSettings.put("ilpOverHttp.outgoing.auth_type", AuthType.SIMPLE.name());
    customSettings.put("ilpOverHttp.outgoing.simple.auth_token", SHH);
    customSettings.put("ilpOverHttp.outgoing.url", newOutgoingUrl(accountId));
    return customSettings;
  }

  private String newOutgoingUrl(String accountId) {
    return testnetHost.toString() + "accounts/" + accountId + "/ilp";
  }

  private Optional<StreamPayment> filterByDestinationAddress(List<StreamPayment> streamPayments,
                                                             InterledgerAddress destinationAddress) {
    return streamPayments.stream()
      .filter(payment -> payment.destinationAddress().equals(destinationAddress))
      .findFirst();
  }

  @Configuration
  protected static class TestConfig {
    @Primary
    @Bean
    public Cache<AccountId, Optional<AccountSettings>> noCachingAccountSettingsCache() {
      // disable caching by setting a very short expiry. we mess with the outgoing settings on the forwarding
      // account in this test, and those can't be cached.
      final Cache<AccountId, Optional<AccountSettings>> accountSettingsCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.NANOSECONDS)
        .build();
      return accountSettingsCache;
    }
  }
}
