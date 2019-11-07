package org.interledger.connector.server.spring.settings;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(connectorSettings.operatorAddress()).isEqualTo((InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.operatorAddress()).isEqualTo((InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.globalPrefix()).isEqualTo((InterledgerAddressPrefix.of("test")));

    // Enabled Protocol Settings
    final EnabledProtocolSettings enabledProtocolSettings = connectorSettings.enabledProtocols();
    assertThat(enabledProtocolSettings.isBlastEnabled()).isEqualTo((true));
    assertThat(enabledProtocolSettings.isPeerConfigEnabled()).isEqualTo((true));
    assertThat(enabledProtocolSettings.isPeerRoutingEnabled()).isEqualTo((true));
    assertThat(enabledProtocolSettings.isPingProtocolEnabled()).isEqualTo((true));
    assertThat(enabledProtocolSettings.isIldcpEnabled()).isEqualTo((true));

    // Enabled Features
    final EnabledFeatureSettings enabledFeatureSettings = connectorSettings.enabledFeatures();
    assertThat(enabledFeatureSettings.isRateLimitingEnabled()).isEqualTo((true));

    // Global Routing Settings
    final GlobalRoutingSettings globalRoutingSettings = connectorSettings.globalRoutingSettings();
    assertThat(globalRoutingSettings.defaultRoute().get()).isEqualTo((AccountId.of("self.internal")));
    assertThat(globalRoutingSettings.isUseParentForDefaultRoute()).isEqualTo((true));
    assertThat(globalRoutingSettings.routingSecret()).isEqualTo(("shh"));
    assertThat(globalRoutingSettings.routeBroadcastInterval()).isEqualTo((Duration.ofMillis(30001L)));
    assertThat(globalRoutingSettings.routeCleanupInterval()).isEqualTo((Duration.ofMillis(30002L)));
    assertThat(globalRoutingSettings.routeExpiry()).isEqualTo((Duration.ofMillis(30003L)));
    assertThat(globalRoutingSettings.maxEpochsPerRoutingTable()).isEqualTo((77));
    // FIXME these don't load from properties anymore
//    assertThat(globalRoutingSettings.staticRoutes().size()).isEqualTo((1));
//    assertThat(globalRoutingSettings.staticRoutes().stream().findFirst().get().prefix())
//        .isEqualTo((InterledgerAddressPrefix.of("test.parent")));
//    assertThat(globalRoutingSettings.staticRoutes().stream().findFirst().get().accountId())
//        .isEqualTo((AccountId.of("bob")));
  }

  @EnableConfigurationProperties(ConnectorSettingsFromPropertyFile.class)
  public static class TestConfiguration {
    // nothing
  }
}
