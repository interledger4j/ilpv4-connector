package org.interledger.ilpv4.connector.persistence.entities;

import org.interledger.connector.accounts.AccountBalanceSettings;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import java.math.BigInteger;
import java.util.Optional;

//@Entity
@Access(AccessType.FIELD)
public class AccountBalanceSettingsEntity implements AccountBalanceSettings {

  @Column(name = "MIN_BALANCE")
  BigInteger minBalance;

  @Column(name = "MAX_BALANCE")
  BigInteger maxBalance;

  @Column(name = "SETTLE_THRESHOLD")
  BigInteger settleThreshold;

  @Column(name = "SETTLE_TO")
  BigInteger settleTo;

  /**
   * To satisfy Hibernate
   */
  AccountBalanceSettingsEntity() {
  }

  public AccountBalanceSettingsEntity(AccountBalanceSettings accountBalanceSettings) {
    this.setMinBalance(accountBalanceSettings.getMinBalance());
    this.setMaxBalance(accountBalanceSettings.getMaxBalance());
    this.setSettleThreshold(accountBalanceSettings.getSettleThreshold());
    this.setSettleTo(accountBalanceSettings.getSettleTo());
  }

  @Override
  public Optional<BigInteger> getMinBalance() {
    return Optional.ofNullable(minBalance);
  }

  public void setMinBalance(Optional<BigInteger> minBalance) {
    this.minBalance = minBalance.orElse(null);
  }

  @Override
  public Optional<BigInteger> getMaxBalance() {
    return Optional.ofNullable(maxBalance);
  }

  public void setMaxBalance(Optional<BigInteger> maxBalance) {
    this.maxBalance = maxBalance.orElse(null);
  }

  @Override
  public Optional<BigInteger> getSettleThreshold() {
    return Optional.ofNullable(settleThreshold);
  }

  public void setSettleThreshold(Optional<BigInteger> settleThreshold) {
    this.settleThreshold = settleThreshold.orElse(null);
  }

  @Override
  public Optional<BigInteger> getSettleTo() {
    return Optional.ofNullable(settleTo);
  }

  public void setSettleTo(Optional<BigInteger> settleTo) {
    this.settleTo = settleTo.orElse(null);
  }
}
