package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.accounts.AccountBalanceSettings;

import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class AccountBalanceSettingsFromPropertyFile implements AccountBalanceSettings {

  private Optional<Long> minBalance = Optional.empty();
  private Optional<Long> settleThreshold = Optional.empty();
  private long settleTo = 0L;

  @Override
  public Optional<Long> getMinBalance() {
    return minBalance;
  }

  public void setMinBalance(Optional<Long> minBalance) {
    this.minBalance = minBalance;
  }

  @Override
  public Optional<Long> getSettleThreshold() {
    return settleThreshold;
  }

  public void setSettleThreshold(Optional<Long> settleThreshold) {
    this.settleThreshold = settleThreshold;
  }

  @Override
  public long getSettleTo() {
    return settleTo;
  }

  public void setSettleTo(long settleTo) {
    this.settleTo = settleTo;
  }
}
