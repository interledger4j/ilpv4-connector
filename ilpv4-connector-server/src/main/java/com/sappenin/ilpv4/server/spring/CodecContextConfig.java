package com.sappenin.ilpv4.server.spring;

import org.interledger.btp.asn.framework.BtpCodecContextFactory;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodecContextConfig {

  public static final String ILP = "ilp";
  public static final String BTP = "btp";

  @Bean
  @Qualifier(ILP)
  CodecContext ilpCodecContext() {
    return InterledgerCodecContextFactory.oer();
  }

  @Bean
  @Qualifier(BTP)
  CodecContext btpCodecContext() {
    return BtpCodecContextFactory.oer();
  }

}