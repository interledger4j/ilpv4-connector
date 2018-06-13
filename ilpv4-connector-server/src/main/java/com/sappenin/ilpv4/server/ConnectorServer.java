package com.sappenin.ilpv4.server;

import com.sappenin.ilpv4.server.support.Server;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Optional;

/**
 * An extension of {@link Server} that implements ILPv4 Connector functionality.
 */
public class ConnectorServer extends Server {

  private final Optional<ConnectorSettings> connectorSettings;

  public ConnectorServer() {
    super(ConnectorServerConfig.class);
    connectorSettings = Optional.empty();
  }

  public ConnectorServer(final ConnectorSettings connectorSettings) {
    super(ConnectorServerConfig.class);
    this.connectorSettings = Optional.of(connectorSettings);
  }

  /**
   * Constructor that allows a {@link ConnectorServer} to be initialized with a different configuration.
   *
   * @param connectorSettings
   * @param configurationOverrides
   */
  public ConnectorServer(ConnectorSettings connectorSettings, Class<?>... configurationOverrides) {
    super(configurationOverrides);
    this.connectorSettings = Optional.of(connectorSettings);
  }

  @Override
  public void start() {
    super.start();

    this.connectorSettings.ifPresent(cs -> {
      ((BeanDefinitionRegistry) this.getContext().getAutowireCapableBeanFactory())
        .removeBeanDefinition("connectorSettings");
      this.getContext().getBeanFactory().registerSingleton("connectorSettings", cs);
    });
  }
}
