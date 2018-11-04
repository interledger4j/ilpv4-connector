package org.interledger.ilpv4.connector.it;

import com.sappenin.ilpv4.IlpConnector;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.interledger.ilpv4.connector.it.BeerCoinArchitecture.*;
import static org.interledger.ilpv4.connector.it.graph.nodes.ConnectorNode.CONNECTOR_BEAN;
import static org.junit.Assert.assertThat;

/**
 * Simple test to verify that the BeerCoin graph is working properly.
 */
public class BeerCoinArchitectureTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(BeerCoinArchitectureTest.class);
  private static Graph graph = BeerCoinArchitecture.beerCoinGraph();

  @BeforeClass
  public static void setup() {
    LOGGER.info("Starting ILP test graph...");
    graph.start();
    LOGGER.info("ILP test graph started!");
  }

  @AfterClass
  public static void shutdown() {
    LOGGER.info("Stopping ILP test graph...");
    graph.stop();
    LOGGER.info("ILP test graph stopped!");
  }

  @Test
  public void testDavidNodeSettings() {
    final IlpConnector davidConnector = getIlpConnectorFromGraph(DAVID);
    assertThat(davidConnector.getConnectorSettings().getIlpAddress(), is(DAVID));

    final IlpConnector adrianConnector = getIlpConnectorFromGraph(ADRIAN);
    assertThat(adrianConnector.getConnectorSettings().getIlpAddress(), is(ADRIAN));

    final IlpConnector jimmieConnector = getIlpConnectorFromGraph(JIMMIE);
    assertThat(jimmieConnector.getConnectorSettings().getIlpAddress(), is(JIMMIE));

    final IlpConnector barConnector = getIlpConnectorFromGraph(BAR_AGRICOLE);
    assertThat(barConnector.getConnectorSettings().getIlpAddress(), is(BAR_AGRICOLE));
  }

  @Test
  public void davidPaysAdrian() {
    final IlpConnector davidConnector = getIlpConnectorFromGraph(DAVID);
    assertThat(davidConnector.getConnectorSettings().getIlpAddress(), is(DAVID));

    //
    //davidConnector.
  }

  /**
   * Helper method to obtain an instance of {@link IlpConnector} from the graph, based upon its Interledger Address.
   *
   * @param interledgerAddress
   *
   * @return
   */
  private IlpConnector getIlpConnectorFromGraph(final InterledgerAddress interledgerAddress) {
    return (IlpConnector) ((ServerNode) graph.getNode(interledgerAddress.getValue()))
      .getServer().getContext().getBean(CONNECTOR_BEAN);
  }

}
