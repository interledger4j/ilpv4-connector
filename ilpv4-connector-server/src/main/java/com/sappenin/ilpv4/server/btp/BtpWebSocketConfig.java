package com.sappenin.ilpv4.server.btp;

import com.sappenin.ilpv4.plugins.PluginManager;
import org.interledger.btp.asn.framework.BtpCodecs;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
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

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@EnableWebSocket
public class BtpWebSocketConfig implements WebSocketConfigurer {

  @Autowired
  Environment environment;

  //  @Autowired
  //  IlpConnector ilpConnector;

  @Autowired
  CodecContext codecContext;

  @Autowired
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

    // Enable any handlers for servers that this Connector is operating.
    final boolean isDevMode = Arrays.stream(environment.getActiveProfiles())
      .filter(profile -> profile.equalsIgnoreCase("mock") || profile.equalsIgnoreCase("dev"))
      .findAny().isPresent();

    // Create a new instance of BtpSocketHandler for listening to incoming connection on a configured port...
    final WebSocketHandler btpSocketHandler = new LoggingWebSocketHandlerDecorator(
      new BtpSocketHandler(codecContext, btpSubProtocolHandlerRegistry)
    );

    // TODO: Connect the WebsocketHandler to a plugin?
    // 1. Get or create BtpPlugin.
    // 2. Create IlpSubProtocolHandler

    final WebSocketHandlerRegistration registeredBtpSocketHandler = registry.addHandler(btpSocketHandler, "/btp");
    if (isDevMode) {
      registeredBtpSocketHandler.setAllowedOrigins("*");
    }
  }

  @Bean
  IlpSubprotocolHandler ilpSubprotocolHandler(CodecContext codecContext, PluginManager pluginManager) {
    return new IlpSubprotocolHandler(codecContext, pluginManager);
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry(final IlpSubprotocolHandler ilpSubprotocolHandler) {
    final BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry();
    registry.putHandler(BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP, ilpSubprotocolHandler);
    return registry;
  }

  @Bean
  CodecContext codecContext() {
    final CodecContext context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES);
    // Connect this context with the BtpCodecs...
    BtpCodecs.register(context);
    return context;
  }

  @PostConstruct
  public void startup() {
  }

}