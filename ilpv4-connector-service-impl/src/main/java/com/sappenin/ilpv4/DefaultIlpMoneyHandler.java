package com.sappenin.ilpv4;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A default implementation of {@link Plugin.IlpMoneyHandler} for use by {@link DefaultIlpConnector} to handle incoming
 * ILP packets.
 */
public class DefaultIlpMoneyHandler implements Plugin.IlpMoneyHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  public DefaultIlpMoneyHandler(final Supplier<ConnectorSettings> connectorSettingsSupplier) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
  }

  // TODO: Make this a supplier.
  public ConnectorSettings getConnectorSettings() {
    return this.connectorSettingsSupplier.get();
  }

  @Override
  public void accept(BigInteger amount) throws InterledgerProtocolException {

  }
}
