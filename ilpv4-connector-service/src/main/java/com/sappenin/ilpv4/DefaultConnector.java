package com.sappenin.ilpv4;

import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * A default implementation of {@link Connector}.
 */
public class DefaultConnector implements Connector {

  private final ConnectorSettings connectorSettings;
  private final AccountManager accountManager;

  public DefaultConnector(final ConnectorSettings connectorSettings, final AccountManager accountManager) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.accountManager = Objects.requireNonNull(accountManager);
  }

  @PostConstruct
  private final void init() {
    // For each account setting, add an account to the AccountManager.
    connectorSettings.getAccounts().stream()
      .map(ConnectorSettings.ConfiguredAccount::toAccount)
      .forEach(accountManager::add);
  }

  @Override
  public Future<InterledgerFulfillPacket> handleIncomingData(
    Account sourceAccount, InterledgerPreparePacket interledgerPreparePacket
  ) throws InterledgerProtocolException {

    throw new RuntimeException("Not yet implemented!");
  }

  @Override
  public Future<Void> handleIncomingMoney(BigInteger amount) {
    return null;
  }
}
