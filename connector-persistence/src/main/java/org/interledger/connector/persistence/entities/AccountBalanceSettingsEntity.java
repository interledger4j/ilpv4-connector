package org.interledger.connector.persistence.entities;

import org.interledger.connector.accounts.AccountBalanceSettings;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Optional;

@Access(AccessType.FIELD)
@Embeddable
public class AccountBalanceSettingsEntity {

  // See Javadoc in AccountSettings for more details around the number types in this class.

  @Column(name = "MIN_BALANCE")
  Long minBalance;

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
    this.setMinBalance(accountBalanceSettings.minBalance());
    this.setSettleThreshold(accountBalanceSettings.settleThreshold());
    this.setSettleTo(accountBalanceSettings.settleTo());
  }

  public Optional<Long> getMinBalance() {
    return Optional.ofNullable(minBalance);
  }

  public void setMinBalance(Optional<Long> minBalance) {
    this.minBalance = minBalance.orElse(null);
  }

  public Optional<Long> getSettleThreshold() {
    return Optional.ofNullable(settleThreshold);
  }

  public void setSettleThreshold(Optional<Long> settleThreshold) {
    this.settleThreshold = settleThreshold.orElse(null);
  }

  public long getSettleTo() {
    return settleTo;
  }

  public void setSettleTo(long settleTo) {
    this.settleTo = settleTo;
  }
}
