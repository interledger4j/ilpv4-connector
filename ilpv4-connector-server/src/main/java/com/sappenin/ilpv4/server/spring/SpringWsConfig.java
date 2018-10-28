package com.sappenin.ilpv4.server.spring;

import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.plugins.btp.BtpSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@EnableWebSocket
public class SpringWsConfig implements WebSocketConfigurer {

  @Autowired
  Environment environment;

  @Autowired
  WebSocketHandler btpWebSocketHandler;

  @Autowired
  Supplier<ConnectorSettings> connectorSettingsSupplier;

  // If no BtpServer is defined, then this value will be set to false, and will not be loaded.
  //private AtomicBoolean btpServerEnabled = new AtomicBoolean(false);

  // Declared as a separate bean so that a plugin can be injected if a particular account is acting as a BTP Server.
  @Bean
  BtpSocketHandler btpSocketHandler() {
    return new BtpSocketHandler();
  }

  @Bean
  WebSocketHandler btpWebSocketHandler(final BtpSocketHandler btpSocketHandler) {
    // Create a new instance of BtpSocketHandler for listening to incoming connection on a configured port...
    return new LoggingWebSocketHandlerDecorator(btpSocketHandler);
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // Enable any filters for servers that this Connector is operating.
    final boolean isDevMode = Arrays.stream(environment.getActiveProfiles())
      .filter(profile -> profile.equalsIgnoreCase("mock") || profile.equalsIgnoreCase("dev"))
      .findAny().isPresent();

    final boolean websocketServerEnabled =
      environment.getProperty(ConnectorSettings.PROPERTY_NAME__WEBSOCKETS_ENABLED, Boolean.class, false);

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