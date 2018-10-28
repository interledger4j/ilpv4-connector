package com.sappenin.ilpv4.server.spring;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BinaryMessageToBtpPacketConverter;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BtpPacketToBinaryMessageConverter;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry;
import com.sappenin.ilpv4.plugins.btp.subprotocols.auth.AuthBtpSubprotocolHandler;
import com.sappenin.ilpv4.plugins.btp.subprotocols.ilp.IlpBtpSubprotocolHandler;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

import static com.sappenin.ilpv4.server.spring.CodecContextConfig.BTP;
import static com.sappenin.ilpv4.server.spring.CodecContextConfig.ILP;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@Import({SpringBtpConfig.ConvertersConfig.class})
public class SpringBtpConfig {

  @Autowired
  Environment environment;

  //  @Autowired
  //  Plugin.IlpDataHandler ilpPluginDataHandler;

  @Autowired
  @Lazy
  IlpPacketSwitch ilpPacketSwitch;

  @Autowired
  IlpBtpSubprotocolHandler ilpBtpSubprotocolHandler;

  @Autowired
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;

  @Autowired
  AuthBtpSubprotocolHandler authBtpSubprotocolHandler;

  @Autowired
  BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  @Autowired
  BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;

  @PostConstruct
  public void startup() {
    // Register the BTP Auth Protocol...
    btpSubProtocolHandlerRegistry
      .putHandler(BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH, authBtpSubprotocolHandler);

    // TODO: Consider injecting the PacketSwitch directly into the ilpBtpSubprotocolHandler. If that works,
    // then this section can be removed.
    // Link the BTP Plugin to the Connector Fabric.
    ilpBtpSubprotocolHandler.setIlpPluginDataHandler(ilpPacketSwitch::sendData);

    btpSubProtocolHandlerRegistry
      .putHandler(BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP, ilpBtpSubprotocolHandler);
  }

  @Bean
  AuthBtpSubprotocolHandler authBtpSubprotocolHandler() {
    return new AuthBtpSubprotocolHandler();
  }

  @Bean
  IlpBtpSubprotocolHandler ilpBtpSubprotocolHandler(@Qualifier(ILP) CodecContext ilpCodecContext) {
    return new IlpBtpSubprotocolHandler(ilpCodecContext);
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry() {
    // All sub-protocols are registered in the Connector's PostContruct!
    return new BtpSubProtocolHandlerRegistry();
  }

  @Configuration
  static class ConvertersConfig {

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
  }

}