package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.GlobalRoutingSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountProviderSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 */
@ConfigurationProperties(prefix = "ilpv4.connector")
@SuppressWarnings("unused")
public class ConnectorSettingsFromPropertyFile implements ConnectorSettings {

  // For override purposes, this is the bean name that is registered in Spring. See ConnectorServer for more details.
  public static final String BEAN_NAME = "ilpv4.connector-com.sappenin.interledger.ilpv4.connector.server.spring" +
    ".settings.properties.ConnectorSettingsFromPropertyFile";

  private InterledgerAddress nodeIlpAddress;

  private EnabledProtocolSettingsFromPropertyFile enabledProtocols =
    new EnabledProtocolSettingsFromPropertyFile();

  private boolean websocketServerEnabled;

  private boolean blastEnabled;

  private InterledgerAddressPrefix globalPrefix = InterledgerAddressPrefix.TEST;

  private GlobalRoutingSettingsFromPropertyFile globalRoutingSettings = new GlobalRoutingSettingsFromPropertyFile();

  private List<AccountSettingsFromPropertyFile> accounts = Lists.newArrayList();

  private List<AccountProviderSettingsFromPropertyFile> accountProviders = Lists.newArrayList();

  public InterledgerAddress getOperatorAddress() {
    return nodeIlpAddress;
  }

  public InterledgerAddress getNodeIlpAddress() {
    return nodeIlpAddress;
  }

  public void setNodeIlpAddress(InterledgerAddress nodeIlpAddress) {
    this.nodeIlpAddress = nodeIlpAddress;
  }

  public InterledgerAddressPrefix getGlobalPrefix() {
    return globalPrefix;
  }

  public void setGlobalPrefix(InterledgerAddressPrefix globalPrefix) {
    this.globalPrefix = globalPrefix;
  }

  public EnabledProtocolSettingsFromPropertyFile getEnabledProtocols() {
    return enabledProtocols;
  }

  public void setEnabledProtocols(EnabledProtocolSettingsFromPropertyFile enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  @Override
  public boolean websocketServerEnabled() {
    return isWebsocketServerEnabled();
  }

  public boolean isWebsocketServerEnabled() {
    return websocketServerEnabled;
  }

  public void setWebsocketServerEnabled(boolean websocketServerEnabled) {
    this.websocketServerEnabled = websocketServerEnabled;
  }

  @Override
  public boolean blastEnabled() {
    return isBlastEnabled();
  }

  public boolean isBlastEnabled() {
    return blastEnabled;
  }

  public void setBlastEnabled(boolean blastEnabled) {
    this.blastEnabled = blastEnabled;
  }

  @Override
  public GlobalRoutingSettings getGlobalRoutingSettings() {
    return globalRoutingSettings;
  }

  public void setGlobalRoutingSettings(GlobalRoutingSettingsFromPropertyFile globalRoutingSettings) {
    this.globalRoutingSettings = globalRoutingSettings;
  }

  @Override
  public List<AccountSettings> getAccountSettings() {
    return accounts.stream()
      .map(accountSettings -> (AccountSettings) accountSettings)
      .collect(Collectors.toList());
  }

  public void setAccounts(List<AccountSettingsFromPropertyFile> accounts) {
    this.accounts = accounts;
  }

  @Override
  public List<AccountProviderSettings> getAccountProviderSettings() {
    return accountProviders.stream()
      .map(accountSettings -> (AccountProviderSettings) accountSettings)
      .collect(Collectors.toList());
  }

  public void setAccountProviders(List<AccountProviderSettingsFromPropertyFile> accountProviders) {
    this.accountProviders = accountProviders;
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class GlobalRoutingSettingsFromPropertyFile implements GlobalRoutingSettings {

    private boolean routeBroadcastEnabled;
    private Optional<AccountId> defaultRoute = Optional.empty();
    private Duration routeCleanupInterval = Duration.ofSeconds(1);
    private Duration routeExpiry = Duration.ofSeconds(45);
    private int maxEpochsPerRoutingTable = 50;
    private String routingSecret;
    private boolean useParentForDefaultRoute;
    private Duration routeBroadcastInterval = Duration.ofSeconds(30);
    private List<StaticRouteFromPropertyFile> staticRoutes = Lists.newArrayList();

    @Override
    public boolean isRouteBroadcastEnabled() {
      return routeBroadcastEnabled;
    }

    public void setRouteBroadcastEnabled(boolean routeBroadcastEnabled) {
      this.routeBroadcastEnabled = routeBroadcastEnabled;
    }

    @Override
    public Optional<AccountId> getDefaultRoute() {
      return defaultRoute;
    }

    public void setDefaultRoute(Optional<AccountId> defaultRoute) {
      this.defaultRoute = defaultRoute;
    }

    @Override
    public Duration getRouteCleanupInterval() {
      return routeCleanupInterval;
    }

    public void setRouteCleanupInterval(Duration routeCleanupInterval) {
      this.routeCleanupInterval = routeCleanupInterval;
    }

    @Override
    public Duration getRouteExpiry() {
      return routeExpiry;
    }

    public void setRouteExpiry(Duration routeExpiry) {
      this.routeExpiry = routeExpiry;
    }

    @Override
    public int getMaxEpochsPerRoutingTable() {
      return maxEpochsPerRoutingTable;
    }

    public void setMaxEpochsPerRoutingTable(int maxEpochsPerRoutingTable) {
      this.maxEpochsPerRoutingTable = maxEpochsPerRoutingTable;
    }

    @Override
    public String getRoutingSecret() {
      return routingSecret;
    }

    public void setRoutingSecret(String routingSecret) {
      this.routingSecret = routingSecret;
    }

    @Override
    public boolean isUseParentForDefaultRoute() {
      return useParentForDefaultRoute;
    }

    public void setUseParentForDefaultRoute(boolean useParentForDefaultRoute) {
      this.useParentForDefaultRoute = useParentForDefaultRoute;
    }

    @Override
    public Duration getRouteBroadcastInterval() {
      return routeBroadcastInterval;
    }

    public void setRouteBroadcastInterval(Duration routeBroadcastInterval) {
      this.routeBroadcastInterval = routeBroadcastInterval;
    }

    @Override
    public List<StaticRouteFromPropertyFile> getStaticRoutes() {
      return staticRoutes;
    }

    public void setStaticRoutes(List<StaticRouteFromPropertyFile> staticRoutes) {
      this.staticRoutes = staticRoutes;
    }
  }
}