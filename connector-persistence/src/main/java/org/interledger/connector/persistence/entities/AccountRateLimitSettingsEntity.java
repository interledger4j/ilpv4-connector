package org.interledger.connector.persistence.entities;

import org.interledger.connector.accounts.AccountRateLimitSettings;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Optional;

@Access(AccessType.FIELD)
@Embeddable
public class AccountRateLimitSettingsEntity {

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

  public Optional<Integer> getMaxPacketsPerSecond() {
    return Optional.ofNullable(maxPacketsPerSecond);
  }

  public void setMaxPacketsPerSecondlance(Optional<Integer> maxPacketsPerSecond) {
    this.maxPacketsPerSecond = maxPacketsPerSecond.orElse(null);
  }
}
