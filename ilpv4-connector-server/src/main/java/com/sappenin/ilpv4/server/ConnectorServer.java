package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.server.support.ConnectorServerConfig;
import com.sappenin.ilpv4.server.support.Server;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Optional;

/**
 * An extension of {@link Server} that implements ILPv4 Connector functionality.
 */
public class ConnectorServer extends Server {

  // Allows a Server to use an overridden ConnectorSettings (useful for ITs).
  private final Optional<ConnectorSettings> connectorSettingsOverride;

  public ConnectorServer() {
    super(ConnectorServerConfig.class);
    this.connectorSettingsOverride = Optional.empty();
  }

  public ConnectorServer(final ConnectorSettings connectorSettings) {
    super(ConnectorServerConfig.class);
    this.connectorSettingsOverride = Optional.of(connectorSettings);
  }

  @Override
  public void start() {
    super.start();

    // Replace the connectorSettings with the supplied variant in order to override what's configured statically via
    // the Server's configuration files or properties...
    this.connectorSettingsOverride.ifPresent(cs -> {
      ((BeanDefinitionRegistry) this.getContext().getAutowireCapableBeanFactory())
        .removeBeanDefinition("connectorSettings");
      this.getContext().getBeanFactory().registerSingleton("connectorSettings", cs);
    });
  }
}
