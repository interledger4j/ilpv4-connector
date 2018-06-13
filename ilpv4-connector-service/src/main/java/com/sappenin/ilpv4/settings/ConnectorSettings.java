package com.sappenin.ilpv4.settings;

import com.google.common.collect.Lists;
import com.sappenin.ilpv4.model.*;
import com.sappenin.ilpv4.plugins.MockPlugin;
import org.interledger.core.InterledgerAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.money.Monetary;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 */
@ConfigurationProperties(prefix = "ilpv4.connector")
public class ConnectorSettings {

  private String ilpAddress;

  private String secret;

  private List<PeerSettings> peers = Lists.newArrayList();

  public String getIlpAddress() {
    return ilpAddress;
  }

  public void setIlpAddress(String ilpAddress) {
    this.ilpAddress = ilpAddress;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public List<PeerSettings> getPeers() {
    return peers;
  }

  public void setPeers(List<PeerSettings> peers) {
    this.peers = peers;
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class PeerSettings {

    private String interledgerAddress;
    private String connectorInterledgerAddress;

    private PeerType peerType = PeerType.PEER;

    private Collection<AccountSettings> accounts = Lists.newArrayList();

    public String getInterledgerAddress() {
      return interledgerAddress;
    }

    public void setInterledgerAddress(String peerId) {
      this.interledgerAddress = peerId;
    }

    public String getConnectorInterledgerAddress() {
      return connectorInterledgerAddress;
    }

    public void setConnectorInterledgerAddress(String connectorInterledgerAddress) {
      this.connectorInterledgerAddress = connectorInterledgerAddress;
    }

    public PeerType getPeerType() {
      return peerType;
    }

    public void setPeerType(PeerType peerType) {
      this.peerType = peerType;
    }

    public Collection<AccountSettings> getAccounts() {
      return accounts;
    }

    public void setAccounts(Collection<AccountSettings> accountSettings) {
      this.accounts = accountSettings;
    }

    public Peer toPeer() {
      final ImmutablePeer.Builder builder = ImmutablePeer.builder();

      builder.interledgerAddress(getInterledgerAddress());
      builder.connectorInterledgerAddress(getConnectorInterledgerAddress());
      builder.relationship(getPeerType());
      builder.accounts(getAccounts().stream()
        .map(AccountSettings::toAccount)
        .collect(Collectors.toList()));

      return builder.build();
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class AccountSettings {

    // The ILP Address that this account correlates to.
    private String interledgerAddress;
    private String connectorInterledgerAddress;

    private String assetCode;

    private Optional<BigInteger> balance = Optional.empty();
    private PluginType pluginType = PluginType.MOCK;
    private Optional<BigInteger> minBalance = Optional.empty();
    private Optional<BigInteger> maxBalance = Optional.empty();
    private int currencyScale = 2;
    private Optional<BigInteger> settleThreshold = Optional.empty();
    private Optional<BigInteger> maximumPacketAmount = Optional.empty();
    private String relationship = PeerType.PEER.name();

    public String getInterledgerAddress() {
      return interledgerAddress;
    }

    public void setInterledgerAddress(String sourceInterledgerAddress) {
      this.interledgerAddress = sourceInterledgerAddress;
    }

    public String getConnectorInterledgerAddress() {
      return connectorInterledgerAddress;
    }

    public void setConnectorInterledgerAddress(String connectorInterledgerAddress) {
      this.connectorInterledgerAddress = connectorInterledgerAddress;
    }

    public Optional<BigInteger> getBalance() {
      return balance;
    }

    public void setBalance(Optional<BigInteger> balance) {
      this.balance = balance;
    }

    public PluginType getPluginType() {
      return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
      this.pluginType = pluginType;
    }

    public Optional<BigInteger> getMinBalance() {
      return minBalance;
    }

    public void setMinBalance(Optional<BigInteger> minBalance) {
      this.minBalance = minBalance;
    }

    public Optional<BigInteger> getMaxBalance() {
      return maxBalance;
    }

    public void setMaxBalance(Optional<BigInteger> maxBalance) {
      this.maxBalance = maxBalance;
    }

    public String getAssetCode() {
      return assetCode;
    }

    public void setAssetCode(String assetCode) {
      this.assetCode = assetCode;
    }

    public int getCurrencyScale() {
      return currencyScale;
    }

    public void setCurrencyScale(int currencyScale) {
      this.currencyScale = currencyScale;
    }

    public Optional<BigInteger> getSettleThreshold() {
      return settleThreshold;
    }

    public void setSettleThreshold(Optional<BigInteger> settleThreshold) {
      this.settleThreshold = settleThreshold;
    }

    public Optional<BigInteger> getMaximumPacketAmount() {
      return maximumPacketAmount;
    }

    public void setMaximumPacketAmount(Optional<BigInteger> maximumPacketAmount) {
      this.maximumPacketAmount = maximumPacketAmount;
    }

    public String getRelationship() {
      return relationship;
    }

    public void setRelationship(String relationship) {
      this.relationship = relationship;
    }

    public Account toAccount() {
      ImmutableAccount.Builder builder = ImmutableAccount.builder();

      builder.interledgerAddress(getInterledgerAddress());
      builder.connectorInterledgerAddress(getConnectorInterledgerAddress());
      builder.plugin(this.constructNewPlugin(
        getConnectorInterledgerAddress(), getInterledgerAddress(), getPluginType()
      ));
      builder.assetCode(Monetary.getCurrency(this.getAssetCode()));
      builder.currencyScale(getCurrencyScale());
      getMinBalance().ifPresent(builder::minBalance);
      getMaxBalance().ifPresent(builder::maxBalance);
      getMaximumPacketAmount().ifPresent(builder::maximumPacketAmount);
      getSettleThreshold().ifPresent(builder::settleThreshold);
      getBalance().ifPresent(builder::balance);
      return builder.build();
    }

    /**
     * Construct a new {@link Plugin} based upon the supplied info.
     *
     * @param interledgerAddress The {@link InterledgerAddress} that this plugin is operating on behalf of.
     * @param pluginType         A {@link PluginType} that corresponds to the type of {@link Plugin} to construct.
     *
     * @return A newly constructed {@link Plugin}.
     */
    private Plugin constructNewPlugin(final String connectorInterledgerAddress, final String interledgerAddress, final
    PluginType pluginType) {
      Objects.requireNonNull(pluginType);

      switch (pluginType) {
        case MOCK: {
          return new MockPlugin(interledgerAddress, connectorInterledgerAddress);
        }
        case BTP: {

        }
        default: {
          throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginType));
        }
      }
    }
  }
}