package com.sappenin.ilpv4.server.spring;

import com.sappenin.ilpv4.IlpConnector;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.plugins.btp.BtpSocketHandler;
import com.sappenin.ilpv4.plugins.btp.BtpSubProtocolHandlerRegistry;
import com.sappenin.ilpv4.plugins.btp.IlpSubprotocolHandler;
import com.sappenin.ilpv4.plugins.btp.converters.BinaryMessageToBtpErrorConverter;
import com.sappenin.ilpv4.plugins.btp.converters.BinaryMessageToBtpMessageConverter;
import com.sappenin.ilpv4.plugins.btp.converters.BinaryMessageToBtpResponseConverter;
import com.sappenin.ilpv4.plugins.btp.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.btp.asn.framework.BtpCodecs;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Import({SpringBtpConfig.ConvertersConfig.class})
@EnableWebSocket
public class SpringBtpConfig implements WebSocketConfigurer {

  @Autowired
  Environment environment;

  //  @Autowired
  //  IlpConnector ilpConnector;

  @Autowired
  AccountManager accountManager;

  @Autowired
  IlpConnector ilpConnector;

  @Autowired
  BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  @Autowired
  BinaryMessageToBtpMessageConverter binaryMessageToBtpMessageConverter;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

    // Enable any handlers for servers that this Connector is operating.
    final boolean isDevMode = Arrays.stream(environment.getActiveProfiles())
      .filter(profile -> profile.equalsIgnoreCase("mock") || profile.equalsIgnoreCase("dev"))
      .findAny().isPresent();

    // Create a new instance of BtpSocketHandler for listening to incoming connection on a configured port...
    final WebSocketHandler btpSocketHandler = new LoggingWebSocketHandlerDecorator(
      new BtpSocketHandler(codecContext(), btpSubProtocolHandlerRegistry(), binaryMessageToBtpMessageConverter,
        btpPacketToBinaryMessageConverter)
    );

    // TODO: Connect the WebsocketHandler to a lpi2?
    // 1. Get or create BtpPlugin.
    // 2. Create IlpSubProtocolHandler

    final WebSocketHandlerRegistration registeredBtpSocketHandler = registry.addHandler(btpSocketHandler, "/btp");
    if (isDevMode) {
      registeredBtpSocketHandler.setAllowedOrigins("*");
    }
  }

  @Bean
  IlpSubprotocolHandler ilpSubprotocolHandler() {
    return new IlpSubprotocolHandler(codecContext(), ilpConnector);
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry() {
    final BtpSubProtocolHandlerRegistry registry = new BtpSubProtocolHandlerRegistry();
    registry.putHandler(BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP, ilpSubprotocolHandler());
    return registry;
  }

  @Bean
  CodecContext codecContext() {
    // Loads all OER plus ILP!
    final CodecContext context = InterledgerCodecContextFactory.oer();

    // Connect this context with the BtpCodecs...
    BtpCodecs.register(context);

    return context;
  }

  @PostConstruct
  public void startup() {
  }

  @Configuration
  static class ConvertersConfig {

    @Bean
    BinaryMessageToBtpResponseConverter binaryMessageToBtpResponseConverter(CodecContext codecContext) {
      return new BinaryMessageToBtpResponseConverter(codecContext);
    }

    @Bean
    BinaryMessageToBtpErrorConverter binaryMessageToBtpErrorConverter(CodecContext codecContext) {
      return new BinaryMessageToBtpErrorConverter(codecContext);
    }

    @Bean
    BinaryMessageToBtpMessageConverter binaryMessageToBtpMessageConverter(CodecContext codecContext) {
      return new BinaryMessageToBtpMessageConverter(codecContext);
    }

    @Bean
    BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter(CodecContext codecContext) {
      return new BtpPacketToBinaryMessageConverter(codecContext);
    }
  }

}