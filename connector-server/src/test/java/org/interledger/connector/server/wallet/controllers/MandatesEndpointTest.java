package org.interledger.connector.server.wallet.controllers;


import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.wallet.controllers.MandatesEndpointTest.TEST_DOT_BOB;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.ilpoverhttp.AbstractEndpointTest;
import org.interledger.connector.server.spsp.LocalSpspClientTestConfig;
import org.interledger.connector.xumm.service.XummPaymentService;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewInvoice;
import org.interledger.openpayments.NewMandate;
import org.interledger.openpayments.PaymentNetwork;
import org.interledger.openpayments.UserAuthorizationRequiredException;
import org.interledger.openpayments.XrpPaymentDetails;
import org.interledger.openpayments.config.OpenPaymentsMetadata;
import org.interledger.openpayments.config.OpenPaymentsSettings;
import org.interledger.openpayments.xrpl.XrplTransaction;
import org.interledger.stream.receiver.ServerSecretSupplier;

import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.HttpUrl;
import org.interleger.openpayments.client.OpenPaymentsClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Tests the connectors ability to send durable STREAM payments via the /accounts/{id}/payments API and fetch
 * payment history.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class, LocalSpspClientTestConfig.class, MandatesEndpointTest.TestConfig.class}
)
@ActiveProfiles( {"test", "test-postgres, wallet-mode"})
// Uses the `application-test.properties` file in the `src/test/resources` folder
@TestPropertySource(
  properties = {
    ConfigConstants.ENABLED_FEATURES + "." + ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED + "=true",
    "interledger.connector.nodeIlpAddress=" + TEST_DOT_BOB
  }
)
@AutoConfigureEmbeddedDatabase
public class MandatesEndpointTest extends AbstractEndpointTest {

  public static final String TEST_DOT_BOB = "test.bob";
  private static final String PAYER = "mandate_test_payer";
  private static final String PAYEE = "nhartner";
  // FIXME make this not depend on an external payid
  private static final String PAYID = "payid:nhartner$stg.payid.xpring.money";

  private static final String USD = "USD";
  private static final String SHH = "shh";

  @MockBean
  private ServerSecretSupplier serverSecretSupplier;

  private OpenPaymentsClient openPaymentsClient;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private HttpUrl testnetHost;

  @Before
  public void setUp() {
    this.testnetHost = HttpUrl.parse("http://localhost:" + localServerPort);
    when(serverSecretSupplier.get()).thenReturn(new byte[32]);

    openPaymentsClient = OpenPaymentsClient.construct(testnetHost.toString());

    adminApiTestClient.findAccount(PAYER).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings(PAYER);
      final AccountSettings senderAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(PAYER))
        .description("Mandate test user")
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

    adminApiTestClient.findAccount(PAYEE).orElseGet(() -> {
      final Map<String, Object> customSettings = constructAccountCustomSettings(PAYEE);
      final AccountSettings senderAccountSettings = AccountSettings.builder()
        .accountId(AccountId.of(PAYEE))
        .description("Mandate test user")
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
  }

  @Test
  public void createMandate() {
    UnsignedLong amount = UnsignedLong.valueOf(100);

    Mandate mandate = openPaymentsClient.createMandate(PAYER,
      bearer(SHH),
      NewMandate.builder()
        .amount(amount)
        .assetCode("XRP")
        .assetScale((short) 9)
        .paymentNetwork(PaymentNetwork.XRPL)
        .build()
    );

    assertThat(mandate.accountId()).isEqualTo(PAYER);
    assertThat(mandate.balance()).isEqualTo(amount);
    assertThat(mandate.id().toString())
      .isEqualTo(testnetHost + PAYER + "/mandates/" + mandate.mandateId());

    Invoice invoice = openPaymentsClient.createInvoice(PAYEE, NewInvoice.builder()
      .amount(amount)
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject(PAYID)
      .description("Fight Milk subscription")
      .ownerAccountUrl(accountUrl(PAYEE))
      .build()
    );

    Charge charge = openPaymentsClient.createCharge(PAYER, mandate.mandateId().value(), bearer(SHH),
      NewCharge.builder()
        .invoice(invoice.receiverInvoiceUrl())
        .build()
      );

    assertThat(charge.amount()).isEqualTo(amount);

    Mandate afterCharge = openPaymentsClient.findMandateById(PAYER,
      mandate.mandateId().value(),
      bearer(SHH)
    );

    assertThat(afterCharge.totalCharged()).isEqualTo(amount);
    assertThat(afterCharge.balance()).isEqualTo(UnsignedLong.ZERO);
  }

  public HttpUrl accountUrl(String account) {
    return this.testnetHost.newBuilder().addPathSegment(account).build();
  }

  @Test
  public void overchargeMandate() {
    UnsignedLong amount = UnsignedLong.valueOf(100);

    Mandate mandate = openPaymentsClient.createMandate(PAYER,
      bearer(SHH),
      NewMandate.builder()
        .amount(amount)
        .assetCode("XRP")
        .assetScale((short) 9)
        .paymentNetwork(PaymentNetwork.XRPL)
        .build()
    );

    assertThat(mandate.accountId()).isEqualTo(PAYER);
    assertThat(mandate.balance()).isEqualTo(amount);

    Invoice invoice = openPaymentsClient.createInvoice(PAYEE, NewInvoice.builder()
      .amount(amount)
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject(PAYID)
      .description("Wolf Cola subscription")
      .ownerAccountUrl(accountUrl(PAYEE))
      .build()
    );

    Charge charge = openPaymentsClient.createCharge(PAYER, mandate.mandateId().value(), bearer(SHH),
      NewCharge.builder()
        .invoice(invoice.receiverInvoiceUrl())
        .build()
    );

    assertThat(charge.amount()).isEqualTo(amount);

    Invoice invoice2 = openPaymentsClient.createInvoice(PAYEE, NewInvoice.builder()
      .amount(amount)
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject(PAYID)
      .description("Troll toll")
      .ownerAccountUrl(accountUrl(PAYEE))
      .build()
    );

    expectedException.expectMessage("Mandate does not have sufficient balance");
    openPaymentsClient.createCharge(PAYER, mandate.mandateId().value(), bearer(SHH),
      NewCharge.builder()
        .invoice(invoice2.receiverInvoiceUrl())
        .build()
    );
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

  @Configuration
  protected static class TestConfig implements ApplicationListener<ServletWebServerInitializedEvent> {

    private int localServerPort;

    @Override
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
      localServerPort = event.getWebServer().getPort();
    }

    @Primary
    @Bean
    public XummPaymentService xummPaymentServiceMock() throws UserAuthorizationRequiredException {
      XummPaymentService mock = mock(XummPaymentService.class);
      when(mock.getDetailsType()).thenReturn(XrpPaymentDetails.class);
      when(mock.getResultType()).thenReturn(XrplTransaction.class);
      when(mock.payInvoice(any(), any(), any(), any()))
        .thenReturn(XrplTransaction.builder()
          .account("mockAccount")
          .amount(UnsignedLong.ONE)
          .hash("FFFF")
          .destination("mockDestination")
          .build()
        );
      when(mock.getPaymentDetails(any())).thenReturn(XrpPaymentDetails.builder()
        .address("notARealXrpAddress")
        .invoiceIdHash("notARealInvoiceHash")
        .build()
      );
      return mock;
    }

    @Primary
    @Bean
    public Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplierOverride() {
      return () -> OpenPaymentsSettings.builder()
        .ilpOperatorAddress(InterledgerAddress.of("example.connector"))
        .metadata(OpenPaymentsMetadata.builder()
          .issuer(HttpUrl.parse("http://localhost:" + localServerPort))
          .build())
        .build();
    }

  }
}
