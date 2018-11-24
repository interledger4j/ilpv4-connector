package org.interledger.ilpv4.connector.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Simple test to verify that the BeerCoin graph is working properly.
 */
public class BeerCoinTopologyTest {

//  private static final Logger LOGGER = LoggerFactory.getLogger(BeerCoinTopologyTest.class);
//  private static Graph graph = BeerCoinTopology.beerCoinGraph();
//
//  @BeforeClass
//  public static void setup() {
//    LOGGER.info("Starting ILP test graph...");
//    graph.start();
//    LOGGER.info("ILP test graph started!");
//  }
//
//  @AfterClass
//  public static void shutdown() {
//    LOGGER.info("Stopping ILP test graph...");
//    graph.stop();
//    LOGGER.info("ILP test graph stopped!");
//  }
//
//  @Test
//  public void testDavidNodeSettings() {
//    final ILPv4Connector davidConnector = getIlpConnectorFromGraph(DAVID);
//    assertThat(davidConnector.getConnectorSettings().getIlpAddress(), is(DAVID));
//
//    final ILPv4Connector adrianConnector = getIlpConnectorFromGraph(ADRIAN);
//    assertThat(adrianConnector.getConnectorSettings().getIlpAddress(), is(ADRIAN));
//
//    final ILPv4Connector jimmieConnector = getIlpConnectorFromGraph(JIMMIE);
//    assertThat(jimmieConnector.getConnectorSettings().getIlpAddress(), is(JIMMIE));
//
//    final ILPv4Connector barConnector = getIlpConnectorFromGraph(BAR_AGRICOLE);
//    assertThat(barConnector.getConnectorSettings().getIlpAddress(), is(BAR_AGRICOLE));
//  }
//
//  @Test
//  public void davidPaysAdrian() {
//    final ILPv4Connector davidConnector = getIlpConnectorFromGraph(DAVID);
//    assertThat(davidConnector.getConnectorSettings().getIlpAddress(), is(DAVID));
//
//    //
//    //davidConnector.
//  }
//
//  /**
//   * Helper method to obtain an instance of {@link ILPv4Connector} from the graph, based upon its Interledger Address.
//   *
//   * @param interledgerAddress
//   *
//   * @return
//   */
//  private ILPv4Connector getIlpConnectorFromGraph(final InterledgerAddress interledgerAddress) {
//    return (ILPv4Connector) ((ServerNode) graph.getNode(interledgerAddress.getValue()))
//      .getServer().getContext().getBean(CONNECTOR_BEAN);
//  }

}
