package com.sappenin.interledger.ilpv4.connector.server;

import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationEvent;

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
  }

  /**
   * Handle an application event.
   *
   * @param event the event to respond to
   */
  @Override
  public void onApplicationEvent(final ApplicationEvent event) {
    if (event instanceof ApplicationPreparedEvent) {
      // If there is a ConnectorSettingsOverride, then add it to the ApplicationContext. The ConnectorConfig is smart
      // enough to detect it and use it instead.
      this.connectorSettingsOverride
        .ifPresent(cso -> ((ApplicationPreparedEvent) event).getApplicationContext().getBeanFactory()
          .registerSingleton(ConnectorSettings.OVERRIDE_BEAN_NAME, cso));

    }
  }
}
