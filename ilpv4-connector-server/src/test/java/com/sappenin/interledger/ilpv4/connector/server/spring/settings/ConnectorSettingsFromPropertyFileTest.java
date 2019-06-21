package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorSettingsFromPropertyFile;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledFeatureSettings;
import com.sappenin.interledger.ilpv4.connector.settings.EnabledProtocolSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountProviderSettings;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
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

    final List<AccountSettings> accounts = connectorSettings.getAccountSettings();
    assertThat(accounts.size(), is(4));

    /////////////////////////
    //  Alice's Account
    /////////////////////////
    {
      final AccountSettings account = accounts.get(0);
      assertThat(account.getAccountId().value(), is("alice"));
      assertThat(account.getDescription(), is("Alice's USD Account"));
      assertThat(account.getLinkType(), is(LoopbackLink.LINK_TYPE));
      assertThat(account.getAssetCode(), is("USD"));
      assertThat(account.getAssetScale(), is(8));
      assertThat(account.isSendRoutes(), is(false));
      assertThat(account.isReceiveRoutes(), is(false));
      assertThat(account.getMaximumPacketAmount(), is(Optional.of(100001L)));
      assertThat(account.getCustomSettings().get("foo"), is("bar"));
      assertThat(account.getCustomSettings().get("boo"), is("baz"));

      final AccountBalanceSettings balanceSettings = account.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.of(1L)));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.of((2L))));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.of(10000001L)));
      assertThat(balanceSettings.getSettleTo(), is(Optional.of(3L)));

      final AccountRateLimitSettings rateLimitSettings = account.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond().get(), is(5000));
    }

    /////////////////////////
    //  Bob's Account
    /////////////////////////
    {
      final AccountSettings account = accounts.get(1);
      assertThat(account.getAccountId().value(), is("bob"));
      assertThat(account.getDescription(), is("Bob's EUR Account"));
      assertThat(account.getLinkType(), is(LoopbackLink.LINK_TYPE));
      assertThat(account.getAssetCode(), is("EUR"));
      assertThat(account.getAssetScale(), is(3));
      assertThat(account.isSendRoutes(), is(true));
      assertThat(account.isReceiveRoutes(), is(true));
      assertThat(account.getMaximumPacketAmount(), is(Optional.of(100002L)));
      assertThat(account.getCustomSettings().get("foo"), is("bar1"));
      assertThat(account.getCustomSettings().get("boo"), is("baz1"));

      final AccountBalanceSettings balanceSettings = account.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.of(-7L)));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.of(-3L)));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.of(-10000001L)));
      assertThat(balanceSettings.getSettleTo(), is(Optional.of(54L)));

      final AccountRateLimitSettings rateLimitSettings = account.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond().get(), is(1));
    }

    /////////////////////////
    //  Empty Account
    /////////////////////////
    {
      final AccountSettings account = accounts.get(2);
      assertThat(account.getAccountId().value(), is("empty"));
      assertThat(account.getDescription(), is(""));
      assertThat(account.getLinkType(), is(nullValue()));
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

      final AccountRateLimitSettings rateLimitSettings = account.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond(), is(Optional.empty()));
    }

    /////////////////////////
    //  Minimal Parent
    /////////////////////////
    {
      final AccountSettings account = accounts.get(3);
      assertThat(account.getAccountId().value(), is("minimal.child"));
      assertThat(account.getDescription(), is("A child account with minimal settings."));
      assertThat(account.getLinkType(), is(LoopbackLink.LINK_TYPE));
      assertThat(account.getAssetCode(), is("USD"));
      assertThat(account.getAssetScale(), is(9));
      assertThat(account.isSendRoutes(), is(false));
      assertThat(account.isReceiveRoutes(), is(false));
      assertThat(account.getMaximumPacketAmount().isPresent(), is(false));
      assertThat(account.getCustomSettings().isEmpty(), is(true));

      final AccountBalanceSettings balanceSettings = account.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleTo(), is(Optional.empty()));

      final AccountRateLimitSettings rateLimitSettings = account.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond(), is(Optional.empty()));
    }

    final List<AccountProviderSettings> accountProviders = connectorSettings.getAccountProviderSettings();
    assertThat(accountProviders.size(), is(3));

    /////////////////////////
    //  testServer1
    /////////////////////////
    {
      final AccountProviderSettings settings = accountProviders.get(0);
      assertThat(settings.getId().value(), is("testServer1"));
      assertThat(settings.getDescription(), is("The testServer1 AccountProvider"));
      assertThat(settings.getLinkType(), is(LoopbackLink.LINK_TYPE));
      assertThat(settings.getAssetCode(), is("USD"));
      assertThat(settings.getAssetScale(), is(8));
      assertThat(settings.isSendRoutes(), is(false));
      assertThat(settings.isReceiveRoutes(), is(false));
      assertThat(settings.getMaximumPacketAmount(), is(Optional.of(100001L)));
      assertThat(settings.getCustomSettings().get("foo"), is("bar"));
      assertThat(settings.getCustomSettings().get("boo"), is("baz"));

      final AccountBalanceSettings balanceSettings = settings.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.of(1L)));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.of(2L)));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.of(10000001L)));
      assertThat(balanceSettings.getSettleTo(), is(Optional.of(3L)));

      final AccountRateLimitSettings rateLimitSettings = settings.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond().get(), is(5000));
    }

    /////////////////////////
    //  empty
    /////////////////////////
    {
      final AccountProviderSettings settings = accountProviders.get(1);
      assertThat(settings.getId().value(), is("empty"));
      assertThat(settings.getDescription(), is(""));
      assertThat(settings.getLinkType(), is(nullValue()));
      assertThat(settings.getAssetCode(), is("USD"));
      assertThat(settings.getAssetScale(), is(2));
      assertThat(settings.isSendRoutes(), is(false));
      assertThat(settings.isReceiveRoutes(), is(false));
      assertThat(settings.getMaximumPacketAmount().isPresent(), is(false));
      assertThat(settings.getCustomSettings().isEmpty(), is(true));

      final AccountBalanceSettings balanceSettings = settings.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleTo(), is(Optional.empty()));

      final AccountRateLimitSettings rateLimitSettings = settings.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond(), is(Optional.empty()));
    }

    /////////////////////////
    //  minimal.parent
    /////////////////////////

    {
      final AccountProviderSettings settings = accountProviders.get(2);
      assertThat(settings.getId().value(), is("minimal.parent"));
      assertThat(settings.getDescription(), is("The minimal.parent AccountProvider"));
      assertThat(settings.getLinkType(), is(LoopbackLink.LINK_TYPE));
      assertThat(settings.getAssetCode(), is("USD"));
      assertThat(settings.getAssetScale(), is(9));
      assertThat(settings.isSendRoutes(), is(false));
      assertThat(settings.isReceiveRoutes(), is(false));
      assertThat(settings.getMaximumPacketAmount().isPresent(), is(false));
      assertThat(settings.getCustomSettings().isEmpty(), is(true));

      final AccountBalanceSettings balanceSettings = settings.getBalanceSettings();
      assertThat(balanceSettings.getMinBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getMaxBalance(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleThreshold(), is(Optional.empty()));
      assertThat(balanceSettings.getSettleTo(), is(Optional.empty()));

      final AccountRateLimitSettings rateLimitSettings = settings.getRateLimitSettings();
      assertThat(rateLimitSettings.getMaxPacketsPerSecond(), is(Optional.empty()));
    }
  }

  @EnableConfigurationProperties(ConnectorSettingsFromPropertyFile.class)
  public static class TestConfiguration {
    // nothing
  }
}