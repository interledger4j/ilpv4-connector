package com.sappenin.ilpv4.server.spring;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.AuthBtpSubprotocolHandler;
import org.interledger.plugin.lpiv2.btp2.subprotocols.ilp.DefaultIlpBtpSubprotocolHandler;
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
  DefaultIlpBtpSubprotocolHandler defaultIlpBtpSubprotocolHandler;

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
    btpSubProtocolHandlerRegistry.putHandler(
      BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH,
      BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM,
      authBtpSubprotocolHandler
    );

    // TODO: Consider injecting the PacketSwitch directly into the connectorIlpBtpSubprotocolHandler. If that works,
    // then this section can be removed.
    // Link the BTP Plugin to the Connector Fabric.
    defaultIlpBtpSubprotocolHandler.setIlpPluginDataHandler(ilpPacketSwitch::sendData);

    btpSubProtocolHandlerRegistry.putHandler(
      BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP,
      BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM,
      defaultIlpBtpSubprotocolHandler
    );
  }

  @Bean
  AuthBtpSubprotocolHandler authBtpSubprotocolHandler() {
    return new AuthBtpSubprotocolHandler();
  }

  @Bean
  DefaultIlpBtpSubprotocolHandler ilpBtpSubprotocolHandler(@Qualifier(ILP) CodecContext ilpCodecContext) {
    return new DefaultIlpBtpSubprotocolHandler(ilpCodecContext);
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