package org.interledger.ilpv4.connector.it;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.Peer;
import com.sappenin.ilpv4.server.ConnectorServer;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.edges.PeeringEdge;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * A graph and setup for simulating the "BeerCoin" token.
 */
public class BeerCoinArchitecture {

  public static final String DAVID = "test.david";
  public static final String ADRIAN = "test.adrian";
  public static final String JIMMIE = "test.jimmie";
  public static final String BAR_AGRICOLE = "test.bar_agricole";

  private static final String DOT = ".";
  private static final String GUIN = "GUIN";
  private static final String DOT_GUIN = DOT + GUIN;
  private static final String SAB = "SAB";
  private static final String DOT_SAB = DOT + SAB;

  static {
    System.setProperty("server.port", "0");
    System.setProperty("spring.jmx.enabled", "false");
    System.setProperty("spring.application.admin.enabled", "false");
  }

  /**
   * <p>A very simple graph that simulates beer tokens that the ILPv4 Connectors <tt>test.david</tt>, <tt>test
   * .adrian</tt> and <tt>test.jimmie</tt> can use to exchange value with each other.</p>
   *
   * <p>In this graph, David has a GUIN account with Adrian (meaning David and Adrian can owe each other any type
   * of Guinness beer). Meanwhile, Adrian has an SAB account with Jimmie (meaning Adrian and Jimmie can owe each other
   * any type of South African Breweries (SAB) beer). Adrian will exchange GUIN for SAB at a rate of 800 to 1000
   * (0.8/1.0). In this way, David can buy Jimmie a <tt>Castle Lager</tt> (the best-known and most popular SAB beer) by
   * using Adrian as a Liquidity provider (no pun intended). To complete this transaction, David pays 800 GUIN to
   * Adrian, who then pays 1000 SAB to Jimmie, who then goes to his favorite watering hole and redeems his 1000 SAB for
   * a Castle Lager.
   *
   * <p>Nodes in this graph are connected as follows:</p>
   *
   * <pre>
   * ┌──────────────┐           ┌──────────────┐          ┌──────────────┐         ┌─────────────────┐
   * │              │           │              │          │              │         │                 │
   * │  test.david  │◁──GUIN───▷│ test.adrian  │◁───SAB──▷│ test.jimmie  │◁──SAB──▷│test.bar_agricole│
   * │              │           │              │          │              │         │                 │
   * └──────────────┘           └──────────────┘          └──────────────┘         └─────────────────┘
   * </pre>
   *
   * @return
   */
  public static final Graph beerCoinGraph() {
    return new Graph()
      // Nodes
      .addNode(DAVID, new ConnectorNode(new ConnectorServer(defaultConnectorSettings(DAVID))))
      .addNode(ADRIAN, new ConnectorNode(new ConnectorServer(defaultConnectorSettings(ADRIAN))))
      .addNode(JIMMIE, new ConnectorNode(new ConnectorServer(defaultConnectorSettings(JIMMIE))))
      .addNode(BAR_AGRICOLE, new ConnectorNode(new ConnectorServer(defaultConnectorSettings(BAR_AGRICOLE))))

      // Edges
      .addEdge(new PeeringEdge(DAVID, peersOfDavid()))
      .addEdge(new PeeringEdge(ADRIAN, peersOfAdrian()))
      .addEdge(new PeeringEdge(JIMMIE, peersOfJimmie()))
      .addEdge(new PeeringEdge(BAR_AGRICOLE, peersOfBar()));
  }

  /**
   * In this Architecture, David peers with Adrian.
   */
  private static List<Peer> peersOfDavid() {
    // Peer with Adrian
    final ConnectorSettings.PeerSettings peerSettings = peerSettings(DAVID, ADRIAN, ADRIAN + DOT_GUIN, GUIN);
    return Lists.newArrayList(peerSettings.toPeer());
  }


  private static ConnectorSettings defaultConnectorSettings(final String ilpAddress) {
    final ConnectorSettings connectorSettings = new ConnectorSettings();
    connectorSettings.setIlpAddress(ilpAddress);
    connectorSettings.setSecret("secret");
    return connectorSettings;
  }

  /**
   * In this Architecture, David peers with Adrian.
   */
  private static List<Peer> peersOfAdrian() {
    final ImmutableList.Builder<Peer> peerListBuilder = ImmutableList.builder();

    {
      // Peer with David
      final ConnectorSettings.PeerSettings peerSettings = peerSettings(ADRIAN, DAVID, DAVID + DOT_GUIN, GUIN);
      peerListBuilder.add(peerSettings.toPeer());
    }

    {
      // Peer with Jimmie
      final ConnectorSettings.PeerSettings peerSettings = peerSettings(ADRIAN, JIMMIE, JIMMIE + DOT_SAB, SAB);
      peerListBuilder.add(peerSettings.toPeer());
    }

    return peerListBuilder.build();
  }

  /**
   * In this Architecture, David peers with Adrian.
   */
  private static List<Peer> peersOfJimmie() {
    // Peer with the Bar.
    final ConnectorSettings.PeerSettings peerSettings = peerSettings(JIMMIE, BAR_AGRICOLE, BAR_AGRICOLE + DOT_SAB, SAB);
    return Lists.newArrayList(peerSettings.toPeer());
  }

  /**
   * In this Architecture, David peers with Adrian.
   */
  private static List<Peer> peersOfBar() {
    // Peer with Jimmie
    final ConnectorSettings.PeerSettings peerSettings = peerSettings(BAR_AGRICOLE, JIMMIE, JIMMIE + DOT_SAB, SAB);
    return Lists.newArrayList(peerSettings.toPeer());
  }

  private static ConnectorSettings.PeerSettings peerSettings(
    final String connectorIlpAddress,
    final String peerIlpAddress, final String accountIlpAddress, final String accountAssetCode
  ) {
    final ConnectorSettings.PeerSettings peerSettings = new ConnectorSettings.PeerSettings();
    peerSettings.setConnectorInterledgerAddress(connectorIlpAddress);
    peerSettings.setInterledgerAddress(peerIlpAddress);
    // account
    final ConnectorSettings.AccountSettings accountSettings = new ConnectorSettings.AccountSettings();
    accountSettings.setConnectorInterledgerAddress(connectorIlpAddress);
    accountSettings.setInterledgerAddress(accountIlpAddress);
    accountSettings.setAssetCode(accountAssetCode);
    peerSettings.getAccounts().add(accountSettings);
    return peerSettings;
  }

  /**
   * Configuration overrides for the <tt>test.david</tt> server.
   */
  private static final class ConnectorSettingsDavid {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ConnectorSettings connectorSettings() {
      return defaultConnectorSettings(DAVID);
    }
  }
}
