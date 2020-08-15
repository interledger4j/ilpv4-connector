package org.interledger.connector.it.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.it.topologies.AbstractTopology.ALICE_CONNECTOR_ADDRESS;
import static org.interledger.connector.it.topologies.startup.OneConnectorTopology.NUM_ACCOUNTS;

import org.interledger.connector.ILPv4Connector;
import org.interledger.connector.it.AbstractIT;
import org.interledger.connector.it.markers.Performance;
import org.interledger.connector.it.topologies.startup.OneConnectorTopology;
import org.interledger.connector.it.topology.Topology;
import org.interledger.core.InterledgerAddress;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>This test adds hundreds of accounts to the datastore and then attempts to start the server in order to ensure
 * that the server becomes responsive, even if startup logic might take a while. </p>
 *
 * <p>For example, in August of 2020, the Xpring Connector was evicted from its K8s cluster, but when it tried to
 * restart, the server did not become responsive before it was evicted from the cluster again, causing a cycle where no
 * Connector instances in the K8s cluster could startup before being evicted. Ultimately, this was caused by a variety
 * of misconfigurations and a route-broadcaster bug that attempted to have `CHILD` accounts propagate routes to
 * themselves (Each account had a 30-second HTTP write timeout, and used the main Connector thread to send this message.
 * When multiplied by hundreds of accounts, serially at startup, the total time to complete this task was _much_ longer
 * than the K8s default timeout).</p>
 *
 * @see "https://github.com/interledger4j/ilpv4-connector/issues/666"
 */
@Category(Performance.class)
public class ConnectorStartupTestIT extends AbstractIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorStartupTestIT.class);

  private static Topology topology = OneConnectorTopology.init();
  private static long elapsedStartupTime;

  private ILPv4Connector aliceConnector;

  @BeforeClass
  public static void startTopology() throws IOException {

    topology.start(); // To load accounts
    topology.stop(); // To restart (for the timing test).

    // Create a MockWebServer. These are lean enough that you can create a new
    // instance for every unit test.
    MockWebServer server = new MockWebServer();
    MockResponse mockResponse = new MockResponse();
    mockResponse.setResponseCode(200);
    mockResponse.setBodyDelay(30, TimeUnit.SECONDS);
    for (int i = 0; i < NUM_ACCOUNTS; i++) {
      server.enqueue(mockResponse);
    }
    server.start(9000);

    LOGGER.info("Initializing topology `{}`...", topology.toString());
    final long startTime = System.nanoTime();
    topology.start(); // Start a 2nd time to actually collect the elapsedStartupTime with accounts present.
    final long endTime = System.nanoTime();
    elapsedStartupTime = endTime - startTime;
    LOGGER.info("Topology `{}` initialized.", topology.toString());
  }

  @AfterClass
  public static void stopTopology() {
    LOGGER.info("Stopping Topology `{}`...", topology.toString());
    topology.stop();
    LOGGER.info("Topology `{}` stopped.", topology.toString());
  }

  @Before
  public void setUp() {
    aliceConnector = this.getILPv4NodeFromGraph(getAliceConnectorAddress());
    assertThat(aliceConnector.getConnectorSettings().operatorAddress()).isEqualTo(getAliceConnectorAddress());
  }

  @Test
  public void testConnectorStartupTime() {
    // Should be enough startup time, even on a slow CI. If this test were to be failing/incorrectly written, it would
    // hang for hours so even a modest alotment here is good enough to validate the fix for #666.
    final long minElapsedStartupTime = 60000L; // 1 min should be enough.
    getLogger().info(
      "MIN elapsed server startup time: {}", TimeUnit.SECONDS.convert(minElapsedStartupTime, TimeUnit.NANOSECONDS)
    );

    long elapsedStartupTimeSeconds = TimeUnit.SECONDS.convert(elapsedStartupTime, TimeUnit.NANOSECONDS);
    getLogger().info("ACTUAL elapsed server startup time: {}", elapsedStartupTimeSeconds);

    assertThat(elapsedStartupTimeSeconds).isLessThan(minElapsedStartupTime);
  }

  /////////////////
  // Helper Methods
  /////////////////

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected Topology getTopology() {
    return topology;
  }

  @Override
  protected InterledgerAddress getAliceConnectorAddress() {
    return ALICE_CONNECTOR_ADDRESS;
  }
}
