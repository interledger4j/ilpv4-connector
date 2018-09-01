package org.interledger.ilpv4.connector.it;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.model.settings.ImmutableAccountSettings;
import com.sappenin.ilpv4.model.settings.ImmutableConnectorSettings;
import com.sappenin.ilpv4.server.ConnectorServer;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.ConnectorNode;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.edges.AccountEdge;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * A graph and setup for simulating the "BeerCoin" token.
 */
public class BeerCoinArchitecture {

  static final InterledgerAddress DAVID = InterledgerAddress.of("test.david");
  static final InterledgerAddress ADRIAN = InterledgerAddress.of("test.adrian");
  static final InterledgerAddress JIMMIE = InterledgerAddress.of("test.jimmie");
  static final InterledgerAddress BAR_AGRICOLE = InterledgerAddress.of("test.bar_agricole");

  static final String GUIN = "GUIN";
  static final String SAB = "SAB";

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
   */
  static Graph beerCoinGraph() {
    final Graph graph = new Graph()
      // Nodes
      .addNode(DAVID.getValue(), new ConnectorNode(new ConnectorServer(defaultConnectorSettings(DAVID))))
      .addNode(ADRIAN.getValue(), new ConnectorNode(new ConnectorServer(defaultConnectorSettings(ADRIAN))))
      .addNode(JIMMIE.getValue(), new ConnectorNode(new ConnectorServer(defaultConnectorSettings(JIMMIE))))
      .addNode(BAR_AGRICOLE.getValue(), new ConnectorNode(new ConnectorServer(defaultConnectorSettings(BAR_AGRICOLE))));

    // Edges
    // Add all of david's accounts as an edge between David and each account.
    accountsOfDavid().stream()
      .forEach(accountSettings -> graph.addEdge(new AccountEdge(DAVID.getValue(), accountSettings)));

    // Add all of adrian's accounts as an edge between Adrian and each account.
    accountsOfAdrian().stream()
      .forEach(accountSettings -> graph.addEdge(new AccountEdge(ADRIAN.getValue(), accountSettings)));

    // Add all of Jimmie's accounts as an edge between Jimmie and each account.
    accountsOfJimmie().stream()
      .forEach(accountSettings -> graph.addEdge(new AccountEdge(JIMMIE.getValue(), accountSettings)));

    // Add all of the Bar's accounts as an edge between the Bar and each account.
    accountsOfBar().stream()
      .forEach(accountSettings -> graph.addEdge(new AccountEdge(BAR_AGRICOLE.getValue(), accountSettings)));

    return graph;
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfDavid() {
    // Peer with Adrian
    final AccountSettings accountSettings = accountSettings(ADRIAN.with(GUIN), GUIN);
    return Lists.newArrayList(accountSettings);
  }

  private static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
    return ImmutableConnectorSettings.builder()
      .ilpAddress(interledgerAddress)
      .secret("secret")
      .build();
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfAdrian() {
    // Peer with David
    final AccountSettings davidAccountSettings = accountSettings(DAVID.with(GUIN), GUIN);

    // Peer with Jimmie
    final AccountSettings jimmieAccountSettings = accountSettings(JIMMIE.with(SAB), SAB);

    return Lists.newArrayList(davidAccountSettings, jimmieAccountSettings);
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfJimmie() {
    // Peer with the Bar.
    final AccountSettings peerSettings = accountSettings(BAR_AGRICOLE.with(SAB), SAB);
    return Lists.newArrayList(peerSettings);
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfBar() {
    // Peer with Jimmie
    final AccountSettings accountSettings = accountSettings(JIMMIE.with(SAB), SAB);
    return Lists.newArrayList(accountSettings);
  }

  private static AccountSettings accountSettings(
    final InterledgerAddress accountIlpAddress, final String accountAssetCode
  ) {
    return ImmutableAccountSettings.builder()
      .interledgerAddress(accountIlpAddress)
      .assetCode(accountAssetCode)
      .build();
  }

  /**
   * Configuration overrides for the <tt>test.david</tt> server.
   */
  @SuppressWarnings("unused")
  private static final class ConnectorSettingsDavid {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ConnectorSettings connectorSettings() {
      return defaultConnectorSettings(DAVID);
    }
  }
}
