package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.sappenin.interledger.ilpv4.connector.settings.AccountBalanceSettings;

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

  public Optional<BigInteger> getSettleThreshold() {
    return settleThreshold;
  }

  public void setSettleThreshold(Optional<BigInteger> settleThreshold) {
    this.settleThreshold = settleThreshold;
  }

  public Optional<BigInteger> getSettleTo() {
    return settleTo;
  }

  public void setSettleTo(Optional<BigInteger> settleTo) {
    this.settleTo = settleTo;
  }
}
