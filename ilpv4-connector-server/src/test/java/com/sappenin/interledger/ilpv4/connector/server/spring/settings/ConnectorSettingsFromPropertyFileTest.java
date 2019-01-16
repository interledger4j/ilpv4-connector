package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorSettingsFromPropertyFile;
import com.sappenin.interledger.ilpv4.connector.settings.AccountBalanceSettings;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.plugin.lpiv2.LoopbackPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.nullValue;
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
    assertThat(connectorSettings.getOperatorAddress(), is(InterledgerAddress.of("test.example")));
    assertThat(connectorSettings.getGlobalPrefix(), is(InterledgerAddressPrefix.of("test")));

    // Global Routing Settings
    final GlobalRoutingSettings globalRoutingSettings = connectorSettings.getGlobalRoutingSettings();
    assertThat(globalRoutingSettings.getDefaultRoute().get(), is(InterledgerAddress.of("self.internal")));
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
      is(InterledgerAddress.of("bob"))
    );

    final List<AccountSettings> accounts = connectorSettings.getAccountSettings();
    assertThat(accounts.size(), is(3));

    /////////////////////////
    //  Alice's Account
    /////////////////////////
    {
      final AccountSettings account = accounts.get(0);
      assertThat(account.getId().value(), is("alice"));
      assertThat(account.getDescription(), is("Alice's USD Account"));
      assertThat(account.getPluginType(), is(LoopbackPlugin.PLUGIN_TYPE));
      assertThat(account.getAssetCode(), is("USD"));
      assertThat(account.getAssetScale(), is(8));
      assertThat(account.isSendRoutes(), is(false));
      assertThat(account.isReceiveRoutes(), is(false));
      assertThat(account.getMaximumPacketAmount(), is(Optional.of(BigInteger.valueOf(100001L))));
      assertThat(account.getCustomSettings().get("foo"), is("bar"));
      assertThat(account.getCustomSettings().get("boo"), is("baz"));

      final AccountBalanceSettings balanceSettings = account.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.of(BigInteger.valueOf(1L))));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.of(BigInteger.valueOf(2L))));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.of(BigInteger.valueOf(10000001L))));
      assertThat(balanceSettings.getSettleTo(), is(Optional.of(BigInteger.valueOf(3L))));
    }

    /////////////////////////
    //  Bob's Account
    /////////////////////////
    {
      final AccountSettings account = accounts.get(1);
      assertThat(account.getId().value(), is("bob"));
      assertThat(account.getDescription(), is("Bob's EUR Account"));
      assertThat(account.getPluginType(), is(LoopbackPlugin.PLUGIN_TYPE));
      assertThat(account.getAssetCode(), is("EUR"));
      assertThat(account.getAssetScale(), is(3));
      assertThat(account.isSendRoutes(), is(true));
      assertThat(account.isReceiveRoutes(), is(true));
      assertThat(account.getMaximumPacketAmount(), is(Optional.of(BigInteger.valueOf(100002L))));
      assertThat(account.getCustomSettings().get("foo"), is("bar1"));
      assertThat(account.getCustomSettings().get("boo"), is("baz1"));

      final AccountBalanceSettings balanceSettings = account.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.of(BigInteger.valueOf(-7L))));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.of(BigInteger.valueOf(-3L))));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.of(BigInteger.valueOf(-10000001L))));
      assertThat(balanceSettings.getSettleTo(), is(Optional.of(BigInteger.valueOf(54L))));
    }

    /////////////////////////
    //  Empty Account
    /////////////////////////
    {
      final AccountSettings account = accounts.get(2);
      assertThat(account.getId().value(), is("empty"));
      assertThat(account.getDescription(), is(""));
      assertThat(account.getPluginType(), is(nullValue()));
      assertThat(account.getAssetCode(), is("USD"));
      assertThat(account.getAssetScale(), is(2));
      assertThat(account.isSendRoutes(), is(false));
      assertThat(account.isReceiveRoutes(), is(false));
      assertThat(account.getMaximumPacketAmount().isPresent(), is(false));
      assertThat(account.getCustomSettings().isEmpty(), is(true));

      final AccountBalanceSettings balanceSettings = account.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleTo(), is(Optional.empty()));
    }
  }

  @EnableConfigurationProperties(ConnectorSettingsFromPropertyFile.class)
  public static class TestConfiguration {
    // nothing
  }
}