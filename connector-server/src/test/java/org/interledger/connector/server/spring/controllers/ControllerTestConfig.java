package org.interledger.connector.server.spring.controllers;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.config.BalanceTrackerConfig;
import org.interledger.connector.config.RedisConfig;
import org.interledger.connector.persistence.config.ConvertersConfig;
import org.interledger.connector.server.spring.settings.CodecContextConfig;
import org.interledger.connector.server.spring.settings.web.IdempotenceCacheConfig;
import org.interledger.encoding.asn.framework.CodecContext;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import( {
  IdempotenceCacheConfig.class,
  RedisConfig.class,
  ConvertersConfig.class,
  BalanceTrackerConfig.class
})
public class ControllerTestConfig {

  @Bean
  @Qualifier(CodecContextConfig.ILP)
  protected CodecContext interledgercodedContext() {
    return InterledgerCodecContextFactory.oer();
  }
}
