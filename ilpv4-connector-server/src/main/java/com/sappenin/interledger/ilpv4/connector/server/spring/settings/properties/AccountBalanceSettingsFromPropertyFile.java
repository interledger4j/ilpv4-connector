package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import org.interledger.connector.accounts.AccountBalanceSettings;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class AccountBalanceSettingsFromPropertyFile implements AccountBalanceSettings {

  private Optional<BigInteger> minBalance = Optional.empty();
  private Optional<BigInteger> maxBalance = Optional.empty();
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
  public Optional<BigInteger> getMaxBalance() {
    return maxBalance;
  }

  public void setMaxBalance(Optional<BigInteger> maxBalance) {
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
