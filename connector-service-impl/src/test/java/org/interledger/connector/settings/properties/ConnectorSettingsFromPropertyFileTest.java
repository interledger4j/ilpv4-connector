package org.interledger.connector.settings.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledFeatureSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.connector.settings.FxConnectionSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.settings.IlpOverHttpConnectionSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.CryptoKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

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
    assertThat(enabledProtocolSettings.isIlpOverHttpEnabled()).isEqualTo((true));
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
    assertThat(globalRoutingSettings.routingSecret()).isEqualTo(Optional.of("shh"));
    assertThat(globalRoutingSettings.routeBroadcastInterval()).isEqualTo((Duration.ofMillis(30001L)));
    assertThat(globalRoutingSettings.routeCleanupInterval()).isEqualTo((Duration.ofMillis(30002L)));
    assertThat(globalRoutingSettings.routeExpiry()).isEqualTo((Duration.ofMillis(30003L)));
    assertThat(globalRoutingSettings.maxEpochsPerRoutingTable()).isEqualTo((77));

    final FxConnectionSettings fxConnectionSettings = connectorSettings.fxSettings().connectionDefaults();
    assertThat(fxConnectionSettings.keepAliveMinutes()).isEqualTo(2);
    assertThat(fxConnectionSettings.connectTimeoutMillis()).isEqualTo(5000);
    assertThat(fxConnectionSettings.maxIdleConnections()).isEqualTo(10);
    assertThat(fxConnectionSettings.readTimeoutMillis()).isEqualTo(30000);
    assertThat(fxConnectionSettings.writeTimeoutMillis()).isEqualTo(40000);

    final IlpOverHttpConnectionSettings ilpOverHttpConnectorSettings = connectorSettings.ilpOverHttpSettings().connectionDefaults();
    assertThat(ilpOverHttpConnectorSettings.connectTimeoutMillis()).isEqualTo(2000);
    assertThat(ilpOverHttpConnectorSettings.readTimeoutMillis()).isEqualTo(70000);
    assertThat(ilpOverHttpConnectorSettings.writeTimeoutMillis()).isEqualTo(50000);
    assertThat(ilpOverHttpConnectorSettings.maxRequests()).isEqualTo(50);
    assertThat(ilpOverHttpConnectorSettings.maxRequestsPerHost()).isEqualTo(75);
    assertThat(ilpOverHttpConnectorSettings.maxIdleConnections()).isEqualTo(12);
    assertThat(ilpOverHttpConnectorSettings.keepAliveSeconds()).isEqualTo(40);

    assertThat(connectorSettings.enabledFeatures().isRequire32ByteSharedSecrets()).isTrue();

    assertThat(connectorSettings.keys().secret0())
      .isEqualTo(CryptoKey.builder().alias("secret0").version("2").build());
    assertThat(connectorSettings.keys().accountSettings())
      .isEqualTo(CryptoKey.builder().alias("accounts").version("3").build());
  }

  @EnableConfigurationProperties(ConnectorSettingsFromPropertyFile.class)
  public static class TestConfiguration {
    @Bean
    public Supplier<ConnectorSettings> connectorSettingsSupplier(ConnectorSettingsFromPropertyFile settings) {
      return () -> settings;
    }

  }
}
