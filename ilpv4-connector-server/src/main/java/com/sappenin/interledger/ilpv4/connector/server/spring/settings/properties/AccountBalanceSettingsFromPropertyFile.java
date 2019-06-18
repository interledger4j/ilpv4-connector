package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import org.interledger.connector.accounts.AccountBalanceSettings;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class AccountBalanceSettingsFromPropertyFile implements AccountBalanceSettings {

  private Optional<Long> minBalance = Optional.empty();
  private Optional<Long> maxBalance = Optional.empty();
  private Optional<Long> settleThreshold = Optional.empty();
  private Optional<Long> settleTo = Optional.empty();

  @Override
  public Optional<Long> getMinBalance() {
    return minBalance;
  }

  public void setMinBalance(Optional<Long> minBalance) {
    this.minBalance = minBalance;
  }

  @Override
  public Optional<Long> getMaxBalance() {
    return maxBalance;
  }

  public void setMaxBalance(Optional<Long> maxBalance) {
    this.maxBalance = maxBalance;
  }

  @Override
  public Optional<Long> getSettleThreshold() {
    return settleThreshold;
  }

  public void setSettleThreshold(Optional<Long> settleThreshold) {
    this.settleThreshold = settleThreshold;
  }

  @Override
  public Optional<Long> getSettleTo() {
    return settleTo;
  }

  public void setSettleTo(Optional<Long> settleTo) {
    this.settleTo = settleTo;
  }
}
