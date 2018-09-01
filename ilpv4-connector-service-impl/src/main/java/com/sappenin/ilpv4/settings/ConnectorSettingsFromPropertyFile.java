package com.sappenin.ilpv4.settings;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.PluginType;
import com.sappenin.ilpv4.model.settings.*;
import org.interledger.core.InterledgerAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 */
@ConfigurationProperties(prefix = "ilpv4.connector")
@SuppressWarnings("unused")
public class ConnectorSettingsFromPropertyFile implements ConnectorSettings {

  private InterledgerAddress ilpAddress;

  private String secret;

  private RouteBroadcastSettingsFromPropertyFile routeBroadcastSettings;

  private List<AccountSettingsFromPropertyFile> accounts = Lists.newArrayList();

  @Override
  public InterledgerAddress getIlpAddress() {
    return ilpAddress;
  }

  public void setIlpAddress(InterledgerAddress ilpAddress) {
    this.ilpAddress = ilpAddress;
  }

  @Override
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * Necessary for Spring to work...
   */
  public List<AccountSettingsFromPropertyFile> getAccounts() {
    return accounts;
  }

  public void setPeers(List<AccountSettingsFromPropertyFile> accounts) {
    this.accounts = accounts;
  }

  @Override
  public RouteBroadcastSettingsFromPropertyFile getRouteBroadcastSettings() {
    return routeBroadcastSettings;
  }

  public void setRouteBroadcastSettings(RouteBroadcastSettingsFromPropertyFile routeBroadcastSettings) {
    this.routeBroadcastSettings = routeBroadcastSettings;
  }

  /**
   * Contains settins for all accounts and associated plugins.
   *
   * @return An instance of {@link AccountSettings}.
   */
  @Override
  public Collection<? extends AccountSettings> getAccountSettings() {
    return this.getAccounts();
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
    private IlpRelationship relationship;

    private PluginType pluginType;

    private String assetCode;
    private int assetScale = 2;

    private AccountBalanceSettingsFromPropertyFile balanceSettings;

    private Optional<BigInteger> maximumPacketAmount = Optional.empty();

    public InterledgerAddress getInterledgerAddress() {
      return interledgerAddress;
    }

    public void setInterledgerAddress(InterledgerAddress sourceInterledgerAddress) {
      this.interledgerAddress = sourceInterledgerAddress;
    }

    @Override
    public IlpRelationship getRelationship() {
      return relationship;
    }

    public void setRelationship(IlpRelationship relationship) {
      this.relationship = relationship;
    }

    @Override
    public AccountBalanceSettingsFromPropertyFile getBalanceSettings() {
      return balanceSettings;
    }

    public void setBalanceSettings(AccountBalanceSettingsFromPropertyFile balanceSettings) {
      this.balanceSettings = balanceSettings;
    }

    @Override
    public PluginType getPluginType() {
      return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
      this.pluginType = pluginType;
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

    private boolean routeBroadcastEnabled;

    private Duration routeBroadcastInterval;

    private Duration routeCleanupInterval;

    private Duration routeExpiry;

    private String routingSecret;

    private Duration minRouteUpdateInterval;

    private int maxEpochsPerRoutingTable;

    public boolean isRouteBroadcastEnabled() {
      return routeBroadcastEnabled;
    }

    public void setRouteBroadcastEnabled(boolean routeBroadcastEnabled) {
      this.routeBroadcastEnabled = routeBroadcastEnabled;
    }

    public Duration getRouteBroadcastInterval() {
      return routeBroadcastInterval;
    }

    public void setRouteBroadcastInterval(Duration routeBroadcastInterval) {
      this.routeBroadcastInterval = routeBroadcastInterval;
    }

    public Duration getRouteCleanupInterval() {
      return routeCleanupInterval;
    }

    public void setRouteCleanupInterval(Duration routeCleanupInterval) {
      this.routeCleanupInterval = routeCleanupInterval;
    }

    public Duration getRouteExpiry() {
      return routeExpiry;
    }

    public void setRouteExpiry(Duration routeExpiry) {
      this.routeExpiry = routeExpiry;
    }

    public String getRoutingSecret() {
      return routingSecret;
    }

    public void setRoutingSecret(String routingSecret) {
      this.routingSecret = routingSecret;
    }

    public Duration getMinRouteUpdateInterval() {
      return minRouteUpdateInterval;
    }

    public void setMinRouteUpdateInterval(Duration minRouteUpdateInterval) {
      this.minRouteUpdateInterval = minRouteUpdateInterval;
    }

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
}