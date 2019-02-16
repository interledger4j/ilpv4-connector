package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.sappenin.interledger.ilpv4.connector.StaticRoute;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddressPrefix;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class StaticRouteFromPropertyFile implements StaticRoute {

  private InterledgerAddressPrefix targetPrefix;
  private AccountId peerAccountId;

  @Override
  public InterledgerAddressPrefix getTargetPrefix() {
    return targetPrefix;
  }

  public void setTargetPrefix(InterledgerAddressPrefix targetPrefix) {
    this.targetPrefix = targetPrefix;
  }

  @Override
  public AccountId getPeerAccountId() {
    return peerAccountId;
  }

  public void setPeerAccountId(AccountId peerAccountId) {
    this.peerAccountId = peerAccountId;
  }
}
