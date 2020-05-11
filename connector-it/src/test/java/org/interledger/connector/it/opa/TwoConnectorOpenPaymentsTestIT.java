package org.interledger.connector.it.opa;

import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_HTTP_BASE_URL;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.AbstractTopology.BOB_HTTP_BASE_URL;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractIlpOverHttpIT;
import org.interledger.connector.it.ContainerHelper;
import org.interledger.connector.it.ilpoverhttp.TwoConnectorMixedAssetCodeTestIT;
import org.interledger.connector.it.markers.OpenPayments;
import org.interledger.connector.it.topologies.ilpoverhttp.TwoConnectorPeerIlpOverHttpTopology;
import org.interledger.connector.it.topology.AbstractBaseTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.wallet.OpenPaymentsClient;
import org.interledger.core.InterledgerAddress;
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
public class TwoConnectorOpenPaymentsTestIT extends AbstractIlpOverHttpIT {

  private static final Network network = Network.newNetwork();

  private static final Logger LOGGER = LoggerFactory.getLogger(TwoConnectorMixedAssetCodeTestIT.class);

  private static Topology topology = TwoConnectorPeerIlpOverHttpTopology.init(
    Denominations.XRP, Denominations.XRP, UnsignedLong.valueOf(1000000000L)
  );

  private static GenericContainer redis = ContainerHelper.redis(network);
  private static GenericContainer postgres = ContainerHelper.postgres(network);

  private ILPv4Connector aliceConnector;
  private ILPv4Connector bobConnector;

  private OpenPaymentsClient client;

  private HttpUrl paulAtAliceInvoicesUri = HttpUrl.get(ALICE_HTTP_BASE_URL + "/paul/invoices");
  private HttpUrl peterAtBobInvoicesUri = HttpUrl.get(BOB_HTTP_BASE_URL + "/peter/invoices");

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
    client = OpenPaymentsClient.construct();
  }

  @Test
  public void createInvoiceOnBob() throws Exception {
//    Thread.sleep(10000);
    Invoice invoice = Invoice.builder()
      .accountId("peter")
      .assetCode(Denominations.XRP.assetCode())
      .assetScale(Denominations.XRP.assetScale())
      .amount(UnsignedLong.valueOf(100))
      .description("IT payment")
      .paymentNetwork(PaymentNetwork.ILP)
      .subject("$localhost/peter")
      .build();

    client.createInvoice(peterAtBobInvoicesUri.uri(), invoice);
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
