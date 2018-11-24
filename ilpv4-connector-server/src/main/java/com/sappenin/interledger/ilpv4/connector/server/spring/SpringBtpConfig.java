package com.sappenin.interledger.ilpv4.connector.server.spring;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static com.sappenin.interledger.ilpv4.connector.server.spring.CodecContextConfig.BTP;

/**
 * Spring configuration for BTP over Websocket.
 */
@Configuration
@Import({SpringBtpConfig.ConvertersConfig.class})
public class SpringBtpConfig {

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