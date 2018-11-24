package org.interledger.ilpv4.connector.it.topologies;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ImmutableAccountSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ImmutableConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.server.ConnectorServer;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.edges.ConnectorAccountEdge;
import org.interledger.ilpv4.connector.it.graph.nodes.interledger.BtpServerPluginNode;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.settings.ImmutablePluginSettings;
import org.interledger.plugin.lpiv2.settings.PluginSettings;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A graph and setup for simulating the "BeerCoin" token, which is a hypothetical digital asset for tokenizing glasses
 * of beer across different geographies that might have differing levels of "beer liquidity."
 */
public class BeerCoinTopology {

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
   * of Guinness beer). Meanwhile, Adrian has an XRP account with Jimmie (meaning Adrian and Jimmie can owe each other
   * any type of South African Breweries (XRP) beer). Adrian will exchange GUIN for XRP at a rate of 800 to 1000
   * (0.8/1.0). In this way, David can buy Jimmie a <tt>Castle Lager</tt> (the best-known and most popular XRP beer) by
   * using Adrian as a Liquidity provider (no pun intended). To complete this transaction, David pays 800 GUIN to
   * Adrian, who then pays 1000 XRP to Jimmie, who then goes to his favorite watering hole and redeems his 1000 XRP for
   * a Castle Lager.
   *
   * <p>Nodes in this graph are connected as follows:</p>
   *
   * <pre>
   * ┌──────────────┐           ┌──────────────┐          ┌──────────────┐         ┌─────────────────┐
   * │              │           │              │          │              │         │                 │
   * │  test.david  │◁──GUIN───▷│ test.adrian  │◁───XRP──▷│ test.jimmie  │◁──XRP──▷│test.bar_agricole│
   * │              │           │              │          │              │         │                 │
   * └──────────────┘           └──────────────┘          └──────────────┘         └─────────────────┘
   * </pre>
   */
  static Graph beerCoinGraph() {
    return new Graph(new Graph.PostConstructListener() {
      @Override
      protected void doAfterGraphStartup(Graph graph) {
        // Edges
        // Add all of david's accounts as an edge between David and each account.
        accountsOfDavid().stream()
          .forEach(accountSettings -> graph.addEdge(new ConnectorAccountEdge(DAVID.getValue(), accountSettings)));

        // Add all of adrian's accounts as an edge between Adrian and each account.
        accountsOfAdrian().stream()
          .forEach(accountSettings -> graph.addEdge(new ConnectorAccountEdge(ADRIAN.getValue(), accountSettings)));

        // Add all of Jimmie's accounts as an edge between Jimmie and each account.
        accountsOfJimmie().stream()
          .forEach(accountSettings -> graph.addEdge(new ConnectorAccountEdge(JIMMIE.getValue(), accountSettings)));

        // Add all of the Bar's accounts as an edge between the Bar and each account.
        accountsOfBar().stream()
          .forEach(
            accountSettings -> graph.addEdge(new ConnectorAccountEdge(BAR_AGRICOLE.getValue(), accountSettings)));
      }
    })
      // Nodes
      .addNode(DAVID.getValue(), new BtpServerPluginNode(DAVID, new ConnectorServer(defaultConnectorSettings(DAVID))))
      .addNode(ADRIAN.getValue(),
        new BtpServerPluginNode(ADRIAN, new ConnectorServer(defaultConnectorSettings(ADRIAN))))
      .addNode(JIMMIE.getValue(),
        new BtpServerPluginNode(JIMMIE, new ConnectorServer(defaultConnectorSettings(JIMMIE))))
      .addNode(BAR_AGRICOLE.getValue(),
        new BtpServerPluginNode(BAR_AGRICOLE, new ConnectorServer(defaultConnectorSettings(BAR_AGRICOLE))));
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfDavid() {
    final Map<String, Object> customSettings = Maps.newConcurrentMap();
    // Peer with Adrian
    final AccountSettings accountSettings = accountSettings(ADRIAN.with(GUIN), GUIN,
      ImmutablePluginSettings.builder()
        // Bob is the BTP Client!
        .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
        .peerAccountAddress(ADRIAN.with(GUIN))
        .localNodeAddress(DAVID)
        .customSettings(customSettings)
        .build()
    );
    return Lists.newArrayList(accountSettings);
  }

  private static ConnectorSettings defaultConnectorSettings(final InterledgerAddress interledgerAddress) {
    return ImmutableConnectorSettings.builder()
      .ilpAddress(interledgerAddress)
      //.secret("secret")
      .build();
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfAdrian() {
    final Map<String, Object> customSettings = Maps.newConcurrentMap();
    //customSettings.put("foo", "bar");

    // Peer with David
    final AccountSettings davidAccountSettings = accountSettings(DAVID.with(GUIN), GUIN,
      ImmutablePluginSettings.builder()
        // Bob is the BTP Client!
        .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
        .peerAccountAddress(DAVID.with(GUIN))
        .localNodeAddress(ADRIAN)
        .customSettings(customSettings)
        .build()
    );

    // Peer with Jimmie
    final AccountSettings jimmieAccountSettings = accountSettings(JIMMIE.with(SAB), SAB,
      ImmutablePluginSettings.builder()
        // Bob is the BTP Client!
        .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
        .peerAccountAddress(JIMMIE.with(SAB))
        .localNodeAddress(ADRIAN)
        .customSettings(customSettings)
        .build()
    );

    return Lists.newArrayList(davidAccountSettings, jimmieAccountSettings);
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfJimmie() {
    final Map<String, Object> customSettings = Maps.newConcurrentMap();
    //customSettings.put("foo", "bar");

    // Peer with the Bar.
    final AccountSettings peerSettings = accountSettings(BAR_AGRICOLE.with(SAB), SAB,
      ImmutablePluginSettings.builder()
        // Bob is the BTP Client!
        .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
        .peerAccountAddress(BAR_AGRICOLE.with(SAB))
        .localNodeAddress(JIMMIE)
        .customSettings(customSettings)
        .build()
    );
    return Lists.newArrayList(peerSettings);
  }

  /**
   * In this Architecture, David accounts with Adrian.
   */
  private static List<AccountSettings> accountsOfBar() {
    final Map<String, Object> customSettings = Maps.newConcurrentMap();

    // Peer with Jimmie
    final AccountSettings accountSettings = accountSettings(JIMMIE.with(SAB), SAB,
      ImmutablePluginSettings.builder()
        // Bob is the BTP Client!
        .pluginType(ClientWebsocketBtpPlugin.PLUGIN_TYPE)
        .peerAccountAddress(JIMMIE.with(SAB))
        .localNodeAddress(BAR_AGRICOLE)
        .customSettings(customSettings)
        .build()
    );
    return Lists.newArrayList(accountSettings);
  }

  private static AccountSettings accountSettings(
    final InterledgerAddress accountIlpAddress, final String accountAssetCode, final PluginSettings pluginSettings
  ) {
    Objects.requireNonNull(accountIlpAddress);
    Objects.requireNonNull(accountAssetCode);
    Objects.requireNonNull(pluginSettings);

    return ImmutableAccountSettings.builder()
      .assetCode(accountAssetCode)
      .pluginSettings(pluginSettings)
      .build();
  }

  //  /**
  //   * Configuration overrides for the <tt>test.david</tt> server.
  //   */
  //  @SuppressWarnings("unused")
  //  private static final class ConnectorSettingsDavid {
  //
  //    @Bean
  //    @Order(Ordered.HIGHEST_PRECEDENCE)
  //    ConnectorSettings connectorSettings() {
  //      return defaultConnectorSettings(DAVID);
  //    }
  //  }
}
