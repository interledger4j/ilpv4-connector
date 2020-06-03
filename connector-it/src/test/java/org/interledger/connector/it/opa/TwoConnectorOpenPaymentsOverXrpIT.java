package org.interledger.connector.it.opa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_HTTP_BASE_URL;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_HTTP_BASE_URL;
import static org.interledger.connector.it.topologies.AbstractTopology.EDDY;
import static org.interledger.connector.it.topologies.AbstractTopology.EDDY_ACCOUNT;
import static org.interledger.connector.it.topologies.AbstractTopology.PAUL;
import static org.interledger.connector.it.topologies.AbstractTopology.PETER;
import static org.interledger.connector.it.topologies.AbstractTopology.PETER_ACCOUNT;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.ilpoverhttp.TwoConnectorMixedAssetCodeTestIT;
import org.interledger.connector.it.markers.OpenPayments;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology;
import org.interledger.connector.it.topology.AbstractBaseTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Memo;
import org.interledger.connector.opa.model.MemoWrapper;
import org.interledger.connector.opa.model.XrplTransaction;
import org.interledger.connector.wallet.OpenPaymentsClient;
import org.interledger.core.InterledgerAddress;
import org.interledger.openpayments.events.ImmutableXrpPaymentCompletedEvent;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;
import org.interledger.stream.Denominations;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(OpenPayments.class)
public class TwoConnectorOpenPaymentsOverXrpIT extends AbstractIlpOverHttpIT {

  private static final Network network = Network.newNetwork();

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorMixedAssetCodeTestIT.class);

  private static Topology topology = TwoConnectorPeerIlpOverHttpTopology.init(
    Denominations.XRP_MILLI_DROPS, Denominations.XRP_MILLI_DROPS, UnsignedLong.valueOf(1000000000L)
  );

  private static GenericContainer redis = ContainerHelper.redis(network);
  private static GenericContainer postgres = ContainerHelper.postgres(network);

  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  private OpenPaymentsClient aliceClient;
  private OpenPaymentsClient bobClient;

  @BeforeClass
  public static void startTopology() {
    LOGGER.info("Starting test topology `{}`...", topology.toString());
    redis.start();
    postgres.start();
    topology.start();
    LOGGER.info("Test topology `{}` started!", topology.toString());
  }

  @AfterClass
  public static void stopTopology() {
    LOGGER.info("Stopping test topology `{}`...", topology.toString());
    topology.stop();
    postgres.stop();
    redis.stop();
    LOGGER.info("Test topology `{}` stopped!", topology.toString());
  }

  @Before
  public void setUp() throws IOException, TimeoutException {
    aliceConnector = this.getILPv4NodeFromGraph(getAliceConnectorAddress());
    // Note Bob's Connector's address is purposefully a child of Alice due to IL-DCP
    bobConnector = this.getILPv4NodeFromGraph(getBobConnectorAddress());
    this.resetBalanceTracking();
    aliceClient = OpenPaymentsClient.construct(ALICE_HTTP_BASE_URL);
    bobClient = OpenPaymentsClient.construct(BOB_HTTP_BASE_URL);
  }

  @Test
  public void peterCreatesInvoiceForHimselfOnBob() {
    Invoice createdInvoice = createInvoiceForPeter(bobClient, PETER);
    assertThat(createdInvoice.invoiceUrl())
      .isNotEmpty()
      .get()
      .isEqualTo(HttpUrl.get("http://localhost:8081/accounts/peter/invoices/" + createdInvoice.id().value()));
    assertThat(createdInvoice.accountId())
      .isEqualTo(PETER);
  }

  @Test
  public void paulCreatesInvoiceForPeterOnBobViaAlice() {
    Invoice createdInvoice = createInvoiceForPeter(aliceClient, PAUL);
    assertThat(createdInvoice.invoiceUrl())
      .isNotEmpty()
      .get()
      .isEqualTo(HttpUrl.get("http://localhost:8081/accounts/peter/invoices/" + createdInvoice.id().value()));
    assertThat(createdInvoice.accountId())
      .isEqualTo(PAUL);
    Invoice invoiceOnBob = bobClient.getInvoice(PETER, createdInvoice.id().value());
    assertThat(invoiceOnBob.accountId())
      .isEqualTo(PETER);
  }

  @Test
  public void peterGetsHisOwnInvoiceOnBob() {
    Invoice createdInvoice = createInvoiceForPeter(bobClient, PETER);
    Invoice getInvoice = bobClient.getInvoice(PETER, createdInvoice.id().value());

    assertThat(createdInvoice).isEqualTo(getInvoice);
  }

  @Test
  public void paulSyncsInvoiceForPeterOnAliceFromBob() {
    Invoice petersInvoiceOnBob = createInvoiceForPeter(bobClient, PETER);

    Invoice synced = aliceClient.getOrSyncInvoice(PAUL, petersInvoiceOnBob.invoiceUrl().get().toString());
    assertThat(petersInvoiceOnBob)
      .isEqualToIgnoringGivenFields(synced, "accountId", "createdAt", "updatedAt");
    assertThat(petersInvoiceOnBob.accountId())
      .isEqualTo(PETER);
    assertThat(synced.accountId())
      .isEqualTo(PAUL);
  }

  @Test
  public void paulPaysInvoiceForPeterViaAliceOnBobOverXrp() throws InterruptedException {
    Invoice createdInvoiceOnBob = createInvoiceForPeter(bobClient, PETER);

    Invoice createdInvoiceOnAlice = aliceClient.getOrSyncInvoice(PAUL, createdInvoiceOnBob.invoiceUrl().get().toString());

    // Manually trigger a payment completed event
    XrplTransaction xprlPayment = XrplTransaction.builder()
      .account("SENDER_ADDRESS")
      .sourceTag("123456")
      .destination("RECEIVER_ADDRESS")
      .destinationTag(123456)
      .amount(createdInvoiceOnAlice.amount())
      .hash("PAYMENT_ID_HASH")
      .addMemos(MemoWrapper.builder()
        .memo(Memo.builder()
          .memoFormat("abcdef")
          .memoType("INVOICE_PAYMENT")
          .memoData(createdInvoiceOnAlice.correlationId().value())
          .build())
        .build())
      .build();
    ImmutableXrpPaymentCompletedEvent xrpPaymentCompletedEvent = XrpPaymentCompletedEvent.builder().payment(xprlPayment).build();
    aliceConnector.getEventBus().post(xrpPaymentCompletedEvent);
    bobConnector.getEventBus().post(xrpPaymentCompletedEvent);

    Thread.sleep(5000);

    Invoice invoiceOnBobAfterPayment = bobClient.getInvoice(createdInvoiceOnBob.accountId().value(), createdInvoiceOnBob.id().value());
    assertThat(invoiceOnBobAfterPayment.isPaid()).isTrue();

    Invoice invoiceOnAliceAfterPayment = aliceClient.getInvoice(createdInvoiceOnAlice.accountId().value(), createdInvoiceOnAlice.id().value());
    assertThat(invoiceOnAliceAfterPayment.isPaid()).isTrue();
  }

  @Test
  public void paulPaysInvoiceForEddyAllOnAlice() throws InterruptedException {
    Invoice invoice = Invoice.builder()
      .accountId(EDDY_ACCOUNT)
      .assetCode(Denominations.XRP_MILLI_DROPS.assetCode())
      .assetScale(Denominations.XRP_MILLI_DROPS.assetScale())
      .amount(UnsignedLong.valueOf(100))
      .subject("eddy$localhost:8080")
      .build();

    Invoice eddyInvoiceOnAlice = aliceClient.createInvoice(EDDY, invoice);
    Invoice syncedInvoice = aliceClient.getOrSyncInvoice(PAUL, eddyInvoiceOnAlice.invoiceUrl().get().toString());
    assertThat(eddyInvoiceOnAlice)
      .isEqualToIgnoringGivenFields(syncedInvoice, "accountId", "createdAt", "updatedAt");
    assertThat(eddyInvoiceOnAlice.accountId())
      .isEqualTo(EDDY);
    assertThat(syncedInvoice.accountId())
      .isEqualTo(PAUL);

    // Manually trigger a payment completed event
    XrplTransaction xprlPayment = XrplTransaction.builder()
      .account("SENDER_ADDRESS")
      .sourceTag("123456")
      .destination("RECEIVER_ADDRESS")
      .destinationTag(123456)
      .amount(syncedInvoice.amount())
      .hash("PAYMENT_ID_HASH")
      .addMemos(MemoWrapper.builder()
        .memo(Memo.builder()
          .memoFormat("abcdef")
          .memoType("INVOICE_PAYMENT")
          .memoData(syncedInvoice.correlationId().value())
          .build())
        .build())
      .build();
    ImmutableXrpPaymentCompletedEvent xrpPaymentCompletedEvent = XrpPaymentCompletedEvent.builder().payment(xprlPayment).build();
    aliceConnector.getEventBus().post(xrpPaymentCompletedEvent);

    // Let the payment system event out and the OP system to update
    Thread.sleep(5000);

    Invoice eddysViewOfTheInvoice = aliceClient.getInvoice(EDDY, eddyInvoiceOnAlice.id().value());
    Invoice paulsViewOfTheInvoice = aliceClient.getInvoice(PAUL, syncedInvoice.id().value());

    assertThat(eddysViewOfTheInvoice)
      .isEqualToIgnoringGivenFields(paulsViewOfTheInvoice, "accountId" ,"createdAt", "updatedAt");
    assertThat(eddysViewOfTheInvoice.received()).isEqualTo(eddysViewOfTheInvoice.amount());
    assertThat(paulsViewOfTheInvoice.received()).isEqualTo(paulsViewOfTheInvoice.amount());
  }

  private Invoice createInvoiceForPeter(OpenPaymentsClient client, String accountId) {
    Invoice invoice = Invoice.builder()
      .accountId(PETER_ACCOUNT)
      .assetCode(Denominations.XRP_DROPS.assetCode())
      .assetScale(Denominations.XRP_DROPS.assetScale())
      .amount(UnsignedLong.valueOf(100))
      .description("IT payment")
      .subject("peter$localhost:8081")
      .build();

    return client.createInvoice(accountId, invoice);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected AbstractBaseTopology getTopology() {
    return topology;
  }

  @Override
  protected InterledgerAddress getAliceConnectorAddress() {
    return ALICE_CONNECTOR_ADDRESS;
  }

  @Override
  protected InterledgerAddress getBobConnectorAddress() {
    return BOB_CONNECTOR_ADDRESS;
  }
}
