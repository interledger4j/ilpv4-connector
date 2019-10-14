package org.interledger.connector.server.spring.settings;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.ildcp.IldcpCodecContextFactory;
import org.interledger.connector.ccp.codecs.CcpCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodecContextConfig {

  public static final String ILP = "interledger";
  //public static final String BTP = "btp";
  public static final String ILDCP = "il-dcp";
  public static final String CCP = "ccp";

  @Bean
  @Qualifier(ILP)
  CodecContext ilpCodecContext() {
    return InterledgerCodecContextFactory.oer();
  }

  @Bean
  @Qualifier(ILDCP)
  CodecContext ildcpCodecContext() {
    return IldcpCodecContextFactory.oer();
  }

  @Bean
  @Qualifier(CCP)
  CodecContext ccpCodecContext() {
    return CcpCodecContextFactory.oer();
  }

  //  @Bean
  //  @Qualifier(BTP)
  //  CodecContext btpCodecContext() {
  //    return BtpCodecContextFactory.oer();
  //  }

}
