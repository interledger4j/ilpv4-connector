package org.interledger.connector.server.spring.settings;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.server.spring.settings.properties.ConnectorSettingsFromPropertyFile;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit test to validate loading of properties into a {@link ConnectorSettingsFromPropertyFile}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConnectorSettingsFromPropertyFileTest.TestConfiguration.class)
@ActiveProfiles("connector-unit-test")
public class ConnectorSettingsFromPropertyFileTest {

  @Autowired
  ConnectorSettingsFromPropertyFile connectorSettings;

  @Test
  public void testConfig() {
    assertThat(connectorSettings.operatorAddress().get(), is(InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.operatorAddressSafe(), is(InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.globalPrefix(), is(InterledgerAddressPrefix.of("test")));

    // Enabled Protocol Settings
    final EnabledProtocolSettings enabledProtocolSettings = connectorSettings.enabledProtocols();
    assertThat(enabledProtocolSettings.isBlastEnabled(), is(true));
    assertThat(enabledProtocolSettings.isPeerConfigEnabled(), is(true));
    assertThat(enabledProtocolSettings.isPeerRoutingEnabled(), is(true));
    assertThat(enabledProtocolSettings.isPingProtocolEnabled(), is(true));
    assertThat(enabledProtocolSettings.isIldcpEnabled(), is(true));

    // Enabled Features
    final EnabledFeatureSettings enabledFeatureSettings = connectorSettings.enabledFeatures();
    assertThat(enabledFeatureSettings.isRateLimitingEnabled(), is(true));

    // Global Routing Settings
    final GlobalRoutingSettings globalRoutingSettings = connectorSettings.globalRoutingSettings();
    assertThat(globalRoutingSettings.defaultRoute().get(), is(AccountId.of("self.internal")));
    assertThat(globalRoutingSettings.isUseParentForDefaultRoute(), is(true));
    assertThat(globalRoutingSettings.routingSecret(), is("shh"));
    assertThat(globalRoutingSettings.routeBroadcastInterval(), is(Duration.ofMillis(30001L)));
    assertThat(globalRoutingSettings.routeCleanupInterval(), is(Duration.ofMillis(30002L)));
    assertThat(globalRoutingSettings.routeExpiry(), is(Duration.ofMillis(30003L)));
    assertThat(globalRoutingSettings.maxEpochsPerRoutingTable(), is(77));
    assertThat(globalRoutingSettings.staticRoutes().size(), is(1));
    assertThat(
      globalRoutingSettings.staticRoutes().stream().findFirst().get().targetPrefix(),
      is(InterledgerAddressPrefix.of("test.parent"))
    );
    assertThat(
      globalRoutingSettings.staticRoutes().stream().findFirst().get().peerAccountId(),
      is(AccountId.of("bob"))
    );
  }

  @EnableConfigurationProperties(ConnectorSettingsFromPropertyFile.class)
  public static class TestConfiguration {
    // nothing
  }
}
