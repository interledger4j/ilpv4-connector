package org.interledger.ilpv4.connector.persistence.entities;

import org.interledger.connector.accounts.AccountBalanceSettings;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Optional;

@Access(AccessType.FIELD)
@Embeddable
public class AccountBalanceSettingsEntity implements AccountBalanceSettings {

  // See Javadoc in AccountSettings for more details around the number types in this class.

  @Column(name = "MIN_BALANCE")
  Long minBalance;

  @Column(name = "MAX_BALANCE")
  Long maxBalance;

  @Column(name = "SETTLE_THRESHOLD")
  Long settleThreshold;

  @Column(name = "SETTLE_TO")
  Long settleTo;

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
  public Optional<Long> getMinBalance() {
    return Optional.ofNullable(minBalance);
  }

  public void setMinBalance(Optional<Long> minBalance) {
    this.minBalance = minBalance.orElse(null);
  }

  @Override
  public Optional<Long> getMaxBalance() {
    return Optional.ofNullable(maxBalance);
  }

  public void setMaxBalance(Optional<Long> maxBalance) {
    this.maxBalance = maxBalance.orElse(null);
  }

  @Override
  public Optional<Long> getSettleThreshold() {
    return Optional.ofNullable(settleThreshold);
  }

  public void setSettleThreshold(Optional<Long> settleThreshold) {
    this.settleThreshold = settleThreshold.orElse(null);
  }

  @Override
  public long getSettleTo() {
    return settleTo;
  }

  public void setSettleTo(long settleTo) {
    this.settleTo = settleTo;
  }
}
