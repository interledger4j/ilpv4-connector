package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.Link;

import com.google.common.collect.Lists;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 * <p>
 * Note that this class supports adding Accounts from a properties file, although these accounts are not accessible from
 * {@link ConnectorSettings}. Instead, all accounts should be accessed via the {@link AccountSettingsRepository}
 * instead.
 */
@ConfigurationProperties(prefix = "interledger.connector")
@SuppressWarnings("unused")
public class ConnectorSettingsFromPropertyFile implements ConnectorSettings {

  // For override purposes, this is the bean name that is registered in Spring. See ConnectorServer for more details.
  public static final String BEAN_NAME = "interledger.connector-org.interledger.connector.server.spring" +
    ".settings.properties.ConnectorSettingsFromPropertyFile";

  // By default, the uninitialized ILP Address is Link.SELF.
  private InterledgerAddress nodeIlpAddress = Link.SELF;

  private EnabledProtocolSettingsFromPropertyFile enabledProtocols =
    new EnabledProtocolSettingsFromPropertyFile();

  private EnabledFeatureSettingsFromPropertyFile enabledFeatures = new EnabledFeatureSettingsFromPropertyFile();

  private boolean websocketServerEnabled;

  private boolean blastEnabled;

  private InterledgerAddressPrefix globalPrefix = InterledgerAddressPrefix.TEST;

  private GlobalRoutingSettingsFromPropertyFile globalRoutingSettings = new GlobalRoutingSettingsFromPropertyFile();

  private int maxHoldTimeMillis = 30000; // default if not set in config

  private int minMessageWindowMillis = 1000;

  private ConnectorKeysFromPropertyFile keys;

  @Override
  public InterledgerAddress operatorAddress() {
    return nodeIlpAddress;
  }

  public InterledgerAddress getNodeIlpAddress() {
    return operatorAddress();
  }

  public void setNodeIlpAddress(InterledgerAddress nodeIlpAddress) {
    this.nodeIlpAddress = nodeIlpAddress;
  }

  @Override
  public InterledgerAddressPrefix globalPrefix() {
    return globalPrefix;
  }

  public void setGlobalPrefix(InterledgerAddressPrefix globalPrefix) {
    this.globalPrefix = globalPrefix;
  }

  @Override
  public EnabledProtocolSettingsFromPropertyFile enabledProtocols() {
    return enabledProtocols;
  }

  public void setEnabledProtocols(EnabledProtocolSettingsFromPropertyFile enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  @Override
  public EnabledFeatureSettingsFromPropertyFile enabledFeatures() {
    return enabledFeatures;
  }

  public void setEnabledFeatures(EnabledFeatureSettingsFromPropertyFile enabledFeatures) {
    this.enabledFeatures = enabledFeatures;
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

  public boolean isBlastEnabled() {
    return blastEnabled;
  }

  public void setBlastEnabled(boolean blastEnabled) {
    this.blastEnabled = blastEnabled;
  }

  @Override
  public GlobalRoutingSettings globalRoutingSettings() {
    return globalRoutingSettings;
  }

  public void setGlobalRoutingSettings(GlobalRoutingSettingsFromPropertyFile globalRoutingSettings) {
    this.globalRoutingSettings = globalRoutingSettings;
  }

  @Override
  public int maxHoldTimeMillis() {
    return maxHoldTimeMillis;
  }

  public void setMaxHoldTimeMillis(int maxHoldTimeMillis) {
    this.maxHoldTimeMillis = maxHoldTimeMillis;
  }

  @Override
  public int minMessageWindowMillis() {
    return minMessageWindowMillis;
  }

  public void setMinMessageWindowMillis(int minMessageWindowMillis) {
    this.minMessageWindowMillis = minMessageWindowMillis;
  }

  @Override
  public ConnectorKeysFromPropertyFile keys() {
    return keys;
  }

  public void setKeys(ConnectorKeysFromPropertyFile keys) {
    this.keys = keys;
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
    public Optional<AccountId> defaultRoute() {
      return defaultRoute;
    }

    public void setDefaultRoute(Optional<AccountId> defaultRoute) {
      this.defaultRoute = defaultRoute;
    }

    @Override
    public Duration routeCleanupInterval() {
      return routeCleanupInterval;
    }

    public void setRouteCleanupInterval(Duration routeCleanupInterval) {
      this.routeCleanupInterval = routeCleanupInterval;
    }

    @Override
    public Duration routeExpiry() {
      return routeExpiry;
    }

    public void setRouteExpiry(Duration routeExpiry) {
      this.routeExpiry = routeExpiry;
    }

    @Override
    public int maxEpochsPerRoutingTable() {
      return maxEpochsPerRoutingTable;
    }

    public void setMaxEpochsPerRoutingTable(int maxEpochsPerRoutingTable) {
      this.maxEpochsPerRoutingTable = maxEpochsPerRoutingTable;
    }

    @Override
    public String routingSecret() {
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
    public Duration routeBroadcastInterval() {
      return routeBroadcastInterval;
    }

    public void setRouteBroadcastInterval(Duration routeBroadcastInterval) {
      this.routeBroadcastInterval = routeBroadcastInterval;
    }

    @Override
    public List<StaticRouteFromPropertyFile> staticRoutes() {
      return staticRoutes;
    }

    public void setStaticRoutes(List<StaticRouteFromPropertyFile> staticRoutes) {
      this.staticRoutes = staticRoutes;
    }
  }
}
