package com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp;

import com.sappenin.interledger.ilpv4.connector.accounts.PluginManager;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.connectorMode.BtpConnectorModeConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.pluginMode.BtpPluginModeConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.conditionals.BtpEnabledCondition;
import org.interledger.btp.BtpResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.spring.BtpClientPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.BtpServerPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.PendingResponseManager;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.spring.factories.BtpClientPluginFactory;
import org.interledger.plugin.lpiv2.btp2.spring.factories.BtpServerPluginFactory;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.AuthBtpSubprotocolHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.BTP;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@Conditional(BtpEnabledCondition.class)
@Import({BtpPluginModeConfig.class, BtpConnectorModeConfig.class}) // Each are conditional...
public class SpringBtpConfig {

  @Autowired
  @Qualifier(BTP)
  CodecContext btpCodecContext;

  @Autowired
  PluginFactoryProvider pluginFactoryProvider;

  @Autowired
  BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;

  @Autowired
  BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  @Autowired
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;

  @Autowired
  PendingResponseManager<BtpResponsePacket> pendingResponseManager;

  @Autowired
  PluginManager pluginManager;

  /**
   * This configuration creates the BTP Plugin Factory, but it needs to be registered with the {@link PluginManager}.
   */
  @PostConstruct
  void startup() {

    ///////////////////////////////////
    // Register BTP PluginFactories....
    ///////////////////////////////////

    // BtpClientPluginFactory
    pluginFactoryProvider.registerPluginFactory(BtpClientPlugin.PLUGIN_TYPE, new BtpClientPluginFactory(
      btpCodecContext,
      binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter,
      btpSubProtocolHandlerRegistry
    ));

    // BtpServerPluginFactory
    pluginFactoryProvider.registerPluginFactory(
      BtpServerPlugin.PLUGIN_TYPE, new BtpServerPluginFactory(
        btpCodecContext,
        binaryMessageToBtpPacketConverter,
        btpPacketToBinaryMessageConverter,
        btpSubProtocolHandlerRegistry,
        pendingResponseManager
      ));
  }

  @Bean
  BinaryMessageToBtpPacketConverter binaryMessageToBtpMessageConverter(
    @Qualifier(BTP) final CodecContext btpCodecContext
  ) {
    return new BinaryMessageToBtpPacketConverter(btpCodecContext);
  }

  @Bean
  BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter(
    @Qualifier(BTP) final CodecContext btpCodecContext
  ) {
    return new BtpPacketToBinaryMessageConverter(btpCodecContext);
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry(final AuthBtpSubprotocolHandler authBtpSubprotocolHandler) {
    return new BtpSubProtocolHandlerRegistry(authBtpSubprotocolHandler);
  }

}