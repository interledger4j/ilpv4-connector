package com.sappenin.interledger.ilpv4.connector.server.spring;

import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import org.interledger.plugin.lpiv2.btp2.spring.BtpBinaryWebSocketHandler;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpAuthenticationService;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.AuthBtpSubprotocolHandler;
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

  // TODO: Connect this to the PluginSettings!
  @Bean
  BtpAuthenticationService btpAuthenticationService() {
    return new BtpAuthenticationService.NoOpBtpAuthenticationService();
  }

  @Bean
  AuthBtpSubprotocolHandler authBtpSubprotocolHandler(final BtpAuthenticationService btpAuthenticationService) {
    return new AuthBtpSubprotocolHandler(btpAuthenticationService);
  }

  //  @Bean
  //  BtpSocketHandler btpSocketHandler(
  //    final AuthBtpSubprotocolHandler authBtpSubprotocolHandler,
  //    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
  //    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter
  //  ) {
  //    return new BtpSocketHandler(authBtpSubprotocolHandler, binaryMessageToBtpPacketConverter,
  //      btpPacketToBinaryMessageConverter);
  //  }

  @Bean
  BtpBinaryWebSocketHandler btpBinaryWebSocketHandler(
    final AuthBtpSubprotocolHandler authBtpSubprotocolHandler,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final Supplier<ConnectorSettings> connectorSettingsSupplier
  ) {
    return new BtpBinaryWebSocketHandler(
      connectorSettingsSupplier.get().getIlpAddress(),
      authBtpSubprotocolHandler, binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter
    );
  }

  @Bean
  WebSocketHandler btpWebSocketHandler(final BtpBinaryWebSocketHandler btpBinaryWebSocketHandler) {
    // Create a new instance of BtpSocketHandler for listening to incoming connection on a configured port...
    return new LoggingWebSocketHandlerDecorator(btpBinaryWebSocketHandler);
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