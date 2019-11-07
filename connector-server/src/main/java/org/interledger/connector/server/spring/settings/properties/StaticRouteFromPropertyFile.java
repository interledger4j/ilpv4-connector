package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import javax.annotation.Nullable;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class StaticRouteFromPropertyFile implements StaticRoute {

  private InterledgerAddressPrefix targetPrefix;
  private AccountId peerAccountId;

  @Nullable
  @Override
  public Long id() {
    return null;
  }

  @Override
  public InterledgerAddressPrefix prefix() {
    return targetPrefix;
  }

  public void setPrefix(InterledgerAddressPrefix targetPrefix) {
    this.targetPrefix = targetPrefix;
  }

  @Override
  public AccountId accountId() {
    return peerAccountId;
  }

  public void setAccountId(AccountId peerAccountId) {
    this.peerAccountId = peerAccountId;
  }
}
