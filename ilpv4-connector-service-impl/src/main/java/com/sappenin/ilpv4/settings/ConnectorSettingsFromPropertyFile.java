package com.sappenin.ilpv4.settings;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.settings.*;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 */
@ConfigurationProperties(prefix = "ilpv4.connector")
@SuppressWarnings("unused")
public class ConnectorSettingsFromPropertyFile implements ConnectorSettings {

  private InterledgerAddress ilpAddress;

  private boolean websocketServerEnabled;

  private InterledgerAddressPrefix globalPrefix;

  private RoutingSettings routingSettings = new RoutingSettingsFromPropertyFile();

  private RouteBroadcastSettings routeBroadcastSettings = new RouteBroadcastSettingsFromPropertyFile();

  private List<AccountSettingsFromPropertyFile> accounts = Lists.newArrayList();

  @Override
  public InterledgerAddress getIlpAddress() {
    return ilpAddress;
  }

  public void setIlpAddress(InterledgerAddress ilpAddress) {
    this.ilpAddress = ilpAddress;
  }

  @Override
  public InterledgerAddressPrefix getGlobalPrefix() {
    return globalPrefix;
  }

  public void setGlobalPrefix(InterledgerAddressPrefix globalPrefix) {
    this.globalPrefix = globalPrefix;
  }

  public boolean websocketServerEnabled() {
    return websocketServerEnabled;
  }

  public void setWebsocketServerEnabled(boolean websocketServerEnabled) {
    this.websocketServerEnabled = websocketServerEnabled;
  }

  @Override
  public RoutingSettings getRoutingSettings() {
    return routingSettings;
  }

  public void setRoutingSettings(RoutingSettings routingSettings) {
    this.routingSettings = routingSettings;
  }

  /**
   * Contains settings for all accounts configured for this Connector.
   *
   * @return An Collection of type {@link AccountSettings}.
   */
  public List<AccountSettings> getAccountSettings() {
    return this.accounts.stream()
      .map(accountSettingsFromPropertyFile -> (AccountSettings) accountSettingsFromPropertyFile).collect(
        Collectors.toList());
  }

  public List<AccountSettingsFromPropertyFile> getAccounts() {
    return accounts;
  }

  public void setAccounts(List<AccountSettingsFromPropertyFile> accounts) {
    this.accounts = accounts;
  }

  @Override
  public RouteBroadcastSettings getRouteBroadcastSettings() {
    return routeBroadcastSettings;
  }

  public void setRouteBroadcastSettings(RouteBroadcastSettings routeBroadcastSettings) {
    this.routeBroadcastSettings = routeBroadcastSettings;
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class AccountBalanceSettingsFromPropertyFile implements AccountBalanceSettings {

    private Optional<BigInteger> minBalance = Optional.empty();
    private BigInteger maxBalance = BigInteger.ZERO;
    private Optional<BigInteger> settleThreshold = Optional.empty();
    private Optional<BigInteger> settleTo = Optional.empty();

    @Override
    public Optional<BigInteger> getMinBalance() {
      return minBalance;
    }

    public void setMinBalance(Optional<BigInteger> minBalance) {
      this.minBalance = minBalance;
    }

    @Override
    public BigInteger getMaxBalance() {
      return maxBalance;
    }

    public void setMaxBalance(BigInteger maxBalance) {
      this.maxBalance = maxBalance;
    }

    @Override
    public Optional<BigInteger> getSettleThreshold() {
      return settleThreshold;
    }

    public void setSettleThreshold(Optional<BigInteger> settleThreshold) {
      this.settleThreshold = settleThreshold;
    }

    @Override
    public Optional<BigInteger> getSettleTo() {
      return settleTo;
    }

    public void setSettleTo(Optional<BigInteger> settleTo) {
      this.settleTo = settleTo;
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class AccountSettingsFromPropertyFile implements AccountSettings {

    // The ILP Address that this account correlates to.
    private InterledgerAddress interledgerAddress;

    private String description;

    private IlpRelationship relationship = IlpRelationship.CHILD;

    private String assetCode = "USD";
    private int assetScale = 2;

    private AccountBalanceSettings balanceSettings = new AccountBalanceSettingsFromPropertyFile();
    private RouteBroadcastSettings routeBroadcastSettings = new RouteBroadcastSettingsFromPropertyFile();
    private PluginSettings pluginSettings = new PluginSettingsFromPropertyFile();

    private Optional<BigInteger> maximumPacketAmount = Optional.empty();

    public InterledgerAddress getInterledgerAddress() {
      return interledgerAddress;
    }

    public void setInterledgerAddress(InterledgerAddress sourceInterledgerAddress) {
      this.interledgerAddress = sourceInterledgerAddress;
    }

    @Override
    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    @Override
    public IlpRelationship getRelationship() {
      return relationship;
    }

    public void setRelationship(IlpRelationship relationship) {
      this.relationship = relationship;
    }

    @Override
    public AccountBalanceSettings getBalanceSettings() {
      return balanceSettings;
    }

    public void setBalanceSettings(AccountBalanceSettings balanceSettings) {
      this.balanceSettings = balanceSettings;
    }

    @Override
    public RouteBroadcastSettings getRouteBroadcastSettings() {
      return routeBroadcastSettings;
    }

    public void setRouteBroadcastSettings(RouteBroadcastSettings routeBroadcastSettings) {
      this.routeBroadcastSettings = routeBroadcastSettings;
    }

    @Override
    public PluginSettings getPluginSettings() {
      return pluginSettings;
    }

    public void setPluginSettings(PluginSettings pluginSettings) {
      this.pluginSettings = pluginSettings;
    }

    @Override
    public String getAssetCode() {
      return assetCode;
    }

    public void setAssetCode(String assetCode) {
      this.assetCode = assetCode;
    }

    @Override
    public int getAssetScale() {
      return assetScale;
    }

    public void setAssetScale(int assetScale) {
      this.assetScale = assetScale;
    }

    @Override
    public Optional<BigInteger> getMaximumPacketAmount() {
      return maximumPacketAmount;
    }

    public void setMaximumPacketAmount(Optional<BigInteger> maximumPacketAmount) {
      this.maximumPacketAmount = maximumPacketAmount;
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class RouteBroadcastSettingsFromPropertyFile implements RouteBroadcastSettings {

    private boolean sendRoutes = true;

    private boolean receiveRoutes = true;

    private Duration routeBroadcastInterval = Duration.ofMillis(30000);

    private Duration routeCleanupInterval = Duration.ofMillis(1000);

    private Duration routeExpiry = Duration.ofMillis(45000);

    private String routingSecret;

    private int maxEpochsPerRoutingTable = 50;

    @Override
    public boolean isSendRoutes() {
      return this.sendRoutes;
    }

    public void setSendRoutes(boolean sendRoutes) {
      this.sendRoutes = sendRoutes;
    }

    @Override
    public boolean isReceiveRoutes() {
      return receiveRoutes;
    }

    public void setReceiveRoutes(boolean receiveRoutes) {
      this.receiveRoutes = receiveRoutes;
    }

    @Override
    public Duration getRouteBroadcastInterval() {
      return routeBroadcastInterval;
    }

    public void setRouteBroadcastInterval(Duration routeBroadcastInterval) {
      this.routeBroadcastInterval = routeBroadcastInterval;
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

    public RouteBroadcastSettings toRouteBroadcastSettings() {
      return ModifiableRouteBroadcastSettings.create().from(this);
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class RoutingSettingsFromPropertyFile implements RoutingSettings {

    private String routingSecret;
    private boolean useParentForDefaultRoute;
    private InterledgerAddress defaultRoute;
    private int maxEpochsPerRoutingTable;
    private List<StaticRouteFromPropertyFile> staticRoutes = Lists.newArrayList();

    @Override
    public String getRoutingSecret() {
      return routingSecret;
    }

    public void setRoutingSecret(String routingSecret) {
      this.routingSecret = routingSecret;
    }

    @Override
    public boolean useParentForDefaultRoute() {
      return useParentForDefaultRoute;
    }

    public void setUseParentForDefaultRoute(boolean useParentForDefaultRoute) {
      this.useParentForDefaultRoute = useParentForDefaultRoute;
    }

    @Override
    public Optional<InterledgerAddress> getDefaultRoute() {
      return Optional.ofNullable(defaultRoute);
    }

    public void setDefaultRoute(InterledgerAddress defaultRoute) {
      this.defaultRoute = defaultRoute;
    }

    @Override
    public List<StaticRouteFromPropertyFile> getStaticRoutes() {
      return staticRoutes;
    }

    public void setStaticRoutes(List<StaticRouteFromPropertyFile> staticRoutes) {
      this.staticRoutes = staticRoutes;
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class PluginSettingsFromPropertyFile implements PluginSettings {

    private PluginType pluginType;
    private InterledgerAddress peerAccountAddress;
    private InterledgerAddress localNodeAddress;
    private Map<String, Object> customSettings;

    @Override
    public PluginType getPluginType() {
      return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
      this.pluginType = pluginType;
    }

    @Override
    public InterledgerAddress getPeerAccountAddress() {
      return peerAccountAddress;
    }

    public void setPeerAccountAddress(InterledgerAddress peerAccountAddress) {
      this.peerAccountAddress = peerAccountAddress;
    }

    @Override
    public InterledgerAddress getLocalNodeAddress() {
      return localNodeAddress;
    }

    public void setLocalNodeAddress(InterledgerAddress localNodeAddress) {
      this.localNodeAddress = localNodeAddress;
    }

    @Override
    public Map<String, Object> getCustomSettings() {
      return customSettings;
    }

    public void setCustomSettings(Map<String, Object> customSettings) {
      this.customSettings = customSettings;
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class StaticRouteFromPropertyFile implements StaticRoute {

    private InterledgerAddressPrefix targetPrefix;
    private InterledgerAddress peerAddress;

    @Override
    public InterledgerAddressPrefix getTargetPrefix() {
      return targetPrefix;
    }

    public void setTargetPrefix(InterledgerAddressPrefix targetPrefix) {
      this.targetPrefix = targetPrefix;
    }

    @Override
    public InterledgerAddress getPeerAddress() {
      return peerAddress;
    }

    public void setPeerAddress(InterledgerAddress peerAddress) {
      this.peerAddress = peerAddress;
    }
  }
}