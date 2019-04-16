package org.interledger.ilpv4.connector.persistence.entities;

import org.interledger.connector.accounts.AccountRateLimitSettings;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import java.util.Optional;

@Access(AccessType.FIELD)
public class AccountRateLimitSettingsEntity implements AccountRateLimitSettings {

  @Column(name = "MAX_PACKETS_PER_SEC")
  Integer maxPacketsPerSecond;

  /**
   * To satisfy Hibernate
   */
  AccountRateLimitSettingsEntity() {
  }

  public AccountRateLimitSettingsEntity(AccountRateLimitSettings accountRateLimitSettings) {
    this.setMaxPacketsPerSecondlance(accountRateLimitSettings.getMaxPacketsPerSecond());
  }

  @Override
  public Optional<Integer> getMaxPacketsPerSecond() {
    return Optional.ofNullable(maxPacketsPerSecond);
  }

  public void setMaxPacketsPerSecondlance(Optional<Integer> maxPacketsPerSecond) {
    this.maxPacketsPerSecond = maxPacketsPerSecond.orElse(null);
  }
}
