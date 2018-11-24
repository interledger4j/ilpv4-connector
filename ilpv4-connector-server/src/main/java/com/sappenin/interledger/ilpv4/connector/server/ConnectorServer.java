package com.sappenin.interledger.ilpv4.connector.server;

import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.server.support.ConnectorServerConfig;
import com.sappenin.interledger.ilpv4.connector.server.support.Server;
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

    // For configuration-override purposes, we in-general need to replace the bean in the ApplicationContext called
    // ConnectorSettings. However, there is no application context available until _after_ the server has started.
    // Thus, some properties are initialized from whatever yaml file happens to be on the class-path, which will
    // initialize certain internal plugins with the wrong information. This is limited to the Connector's ILPAddress
    // and routing prefix, so those are preemptively set via property overrides here.
    this.connectorSettingsOverride.ifPresent(cso -> {
      this.setProperty("ilpv4.connector.ilpAddress", cso.getIlpAddress().getValue());
      this.setProperty("ilpv4.connector.globalPrefix", cso.getGlobalPrefix().getValue());
    });

    super.start();

    // Replace the connectorSettings with the supplied variant in order to override what's configured statically via
    // the Server's configuration files or properties...
    this.connectorSettingsOverride.ifPresent(cso -> {
      final BeanDefinitionRegistry registry = (
        (BeanDefinitionRegistry) this.getContext().getAutowireCapableBeanFactory()
      );
      registry.removeBeanDefinition(ConnectorSettings.BEAN_NAME);
      // Replace here...
      this.getContext().getBeanFactory().registerSingleton(ConnectorSettings.BEAN_NAME, cso);
    });
  }
}
