package org.interledger.connector.server.spring.controllers;

import static org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig.ILP_OVER_HTTP;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.config.BalanceTrackerConfig;
import org.interledger.connector.config.RedisConfig;
import org.interledger.connector.persistence.config.ConvertersConfig;
import org.interledger.connector.server.Application;
import org.interledger.connector.server.spring.settings.CodecContextConfig;
import org.interledger.connector.server.spring.settings.properties.ConnectorSettingsFromPropertyFile;
import org.interledger.connector.server.spring.settings.web.IdempotenceCacheConfig;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.IlpOverHttpConnectionSettings;
import org.interledger.connector.settings.IlpOverHttpConnectorSettings;
import org.interledger.connector.settings.ImmutableConnectorSettings;
import org.interledger.encoding.asn.framework.CodecContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.function.Supplier;

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
