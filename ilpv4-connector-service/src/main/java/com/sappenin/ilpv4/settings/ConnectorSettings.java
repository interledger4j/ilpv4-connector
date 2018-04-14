package com.sappenin.ilpv4.settings;

import com.sappenin.ilpv4.model.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.money.Monetary;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 */
@ConfigurationProperties(prefix = "ilpv4.connector")
public class ConnectorSettings {

  private String ilpAddress;

  private String secret;

  private List<ConfiguredAccount> accounts;

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

  public List<ConfiguredAccount> getAccounts() {
    return accounts;
  }

  public void setAccounts(List<ConfiguredAccount> accounts) {
    this.accounts = accounts;
  }

  /**
   * Models the YAML format for spring-boot automatic configuration propery loading.
   */
  public static class ConfiguredAccount {

    private UUID accountId;
    private Optional<BigInteger> balance;
    private String pluginId;
    private ConfiguredAccountOptions accountOptions;

    public UUID getAccountId() {
      return accountId;
    }

    public void setAccountId(UUID accountId) {
      this.accountId = accountId;
    }

    public Optional<BigInteger> getBalance() {
      return balance;
    }

    public void setBalance(Optional<BigInteger> balance) {
      this.balance = balance;
    }

    public ConfiguredAccountOptions getAccountOptions() {
      return accountOptions;
    }

    public void setAccountOptions(ConfiguredAccountOptions configuredAccountOptions) {
      this.accountOptions = configuredAccountOptions;
    }

    public String getPluginId() {
      return pluginId;
    }

    public void setPluginId(String pluginId) {
      this.pluginId = pluginId;
    }

    public Account toAccount() {
      ImmutableAccount.Builder builder = ImmutableAccount.builder();

      builder.accountId(AccountId.of(getAccountId()));
      builder.accountOptions(getAccountOptions().toAccountOptions());

      getBalance().ifPresent(builder::balance);
      return builder.build();
    }
  }

  /**
   * Models the YAML format for spring-boot automatic configuration property loading.
   */
  public static class ConfiguredAccountOptions {

    private Optional<BigInteger> minBalance;
    private Optional<BigInteger> maxBalance;
    private String assetCode;
    private int currencyScale;
    private Optional<BigInteger> settleThreshold;
    private Optional<BigInteger> maximumPacketAmount;
    private String relationship;

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

    public AccountOptions toAccountOptions() {
      final ImmutableAccountOptions.Builder builder = ImmutableAccountOptions.builder();

      builder.relationship(AccountRelationship.valueOf(getRelationship().toUpperCase()));
      builder.currencyScale(getCurrencyScale());
      builder.assetCode(Monetary.getCurrency(this.getAssetCode()));

      getMinBalance().ifPresent(builder::minBalance);
      getMaxBalance().ifPresent(builder::maxBalance);
      getMaximumPacketAmount().ifPresent(builder::maximumPacketAmount);
      getSettleThreshold().ifPresent(builder::settleThreshold);

      return builder.build();
    }
  }
}