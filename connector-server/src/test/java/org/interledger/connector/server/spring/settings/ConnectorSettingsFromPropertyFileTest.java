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
    assertThat(connectorSettings.getOperatorAddress().get(), is(InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.getOperatorAddressSafe(), is(InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.getGlobalPrefix(), is(InterledgerAddressPrefix.of("test")));

    // Enabled Protocol Settings
    final EnabledProtocolSettings enabledProtocolSettings = connectorSettings.getEnabledProtocols();
    assertThat(enabledProtocolSettings.isBlastEnabled(), is(true));
    assertThat(enabledProtocolSettings.isPeerConfigEnabled(), is(true));
    assertThat(enabledProtocolSettings.isPeerRoutingEnabled(), is(true));
    assertThat(enabledProtocolSettings.isPingProtocolEnabled(), is(true));
    assertThat(enabledProtocolSettings.isIldcpEnabled(), is(true));

    // Enabled Features
    final EnabledFeatureSettings enabledFeatureSettings = connectorSettings.getEnabledFeatures();
    assertThat(enabledFeatureSettings.isRateLimitingEnabled(), is(true));

    // Global Routing Settings
    final GlobalRoutingSettings globalRoutingSettings = connectorSettings.getGlobalRoutingSettings();
    assertThat(globalRoutingSettings.getDefaultRoute().get(), is(AccountId.of("self.internal")));
    assertThat(globalRoutingSettings.isUseParentForDefaultRoute(), is(true));
    assertThat(globalRoutingSettings.getRoutingSecret(), is("shh"));
    assertThat(globalRoutingSettings.getRouteBroadcastInterval(), is(Duration.ofMillis(30001L)));
    assertThat(globalRoutingSettings.getRouteCleanupInterval(), is(Duration.ofMillis(30002L)));
    assertThat(globalRoutingSettings.getRouteExpiry(), is(Duration.ofMillis(30003L)));
    assertThat(globalRoutingSettings.getMaxEpochsPerRoutingTable(), is(77));
    assertThat(globalRoutingSettings.getStaticRoutes().size(), is(1));
    assertThat(
      globalRoutingSettings.getStaticRoutes().stream().findFirst().get().getTargetPrefix(),
      is(InterledgerAddressPrefix.of("test.parent"))
    );
    assertThat(
      globalRoutingSettings.getStaticRoutes().stream().findFirst().get().getPeerAccountId(),
      is(AccountId.of("bob"))
    );
  }

  @EnableConfigurationProperties(ConnectorSettingsFromPropertyFile.class)
  public static class TestConfiguration {
    // nothing
  }
}
