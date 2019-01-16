package com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.connectorMode;

import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@Profile(ConnectorProfile.CONNECTOR_MODE) // Only run a websocket server in `CONNECTOR_MODE` mode.
@EnableWebSocket
public class SpringWebsocketServerConfig implements WebSocketConfigurer {

  @Autowired
  Environment environment;

  @Autowired
  WebSocketHandler btpWebSocketHandler;

  @Value(ConnectorProperties.WEBSOCKET_SERVER_ENABLED)
  boolean websocketServerEnabled;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // Enable any filters for servers that this Connector is operating.
    final boolean isDevMode = Arrays.stream(environment.getActiveProfiles())
      .filter(profile -> profile.equalsIgnoreCase(ConnectorProfile.DEV))
      .findAny().isPresent();

    // Can't rely upon the ConnectorSettings, because at this point in the initialization, its value is the one take
    // from the properties file, which may be overidden at runtime.
    if (websocketServerEnabled) {

      // TODO: Instead of doing it this way via late-binding, we can remove the BtpSocketHandler, and always create
      // an instance of BtpServerPlugin, and Inject it into SpringWsConfig. If Websockets are turned on, we can
      // connect the plugin to the WebsocketHandler directly in the config. This will drastically simplify the design
      // . If we want to operate something like mini-accounts, we should prefer gRPC and not try to make BTP conform
      // to a design it wasn't meant to support.

      // TODO: The current JS connector has a single port per plugin. If that design holds, we need to register new
      // filters for each server-BTP plugin. Before doing that, however, let's research the types of plugins that will
      // be built, and see if this is actually needed. See TODO in BtpSocketHandler.
      final WebSocketHandlerRegistration registeredBtpSocketHandler = registry.addHandler(btpWebSocketHandler, "/btp");
      if (isDevMode) {
        registeredBtpSocketHandler.setAllowedOrigins("*");
      }
    }
  }

}