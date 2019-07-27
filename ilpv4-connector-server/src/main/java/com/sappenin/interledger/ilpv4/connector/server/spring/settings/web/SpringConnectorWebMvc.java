package com.sappenin.interledger.ilpv4.connector.server.spring.settings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.AccountBalanceSettingsConverter;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.AccountSettingsConverter;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.OerPreparePacketHttpMessageConverter;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.RateLimitSettingsConverter;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters.SettlementEngineDetailsConverter;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast.BlastConfig;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.FormatterRegistry;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties.ENABLED_PROTOCOLS;

/**
 * Web config for the Spring Connector.
 */
@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = BLAST_ENABLED, havingValue = "true")
@EnableWebMvc
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL})
@ComponentScan(basePackages = "com.sappenin.interledger.ilpv4.connector.server.spring.controllers")
@Import({BlastConfig.class, JacksonConfig.class, SecurityConfiguration.class})
public class SpringConnectorWebMvc implements WebMvcConfigurer {

  // TODO: Configure TLS
  // TODO: Configure HTTP/2
  @Autowired
  @Qualifier(ILP)
  private CodecContext ilpCodecContext;

  @Autowired
  private ObjectMapper objectMapper;

  ////////////////////////
  // SpringConverters
  ////////////////////////

  @Bean
  RateLimitSettingsConverter rateLimitSettingsConverter() {
    return new RateLimitSettingsConverter();
  }

  @Bean
  AccountBalanceSettingsConverter accountBalanceSettingsConverter() {
    return new AccountBalanceSettingsConverter();
  }

  @Bean
  SettlementEngineDetailsConverter settlementEngineDetailsConverter() {
    return new SettlementEngineDetailsConverter();
  }

  @Bean
  AccountSettingsConverter accountSettingsConverter(
  ) {
    return new AccountSettingsConverter(
      rateLimitSettingsConverter(), accountBalanceSettingsConverter(), settlementEngineDetailsConverter()
    );
  }


  ////////////////////////
  // HttpMessageConverters
  ////////////////////////

  @Bean
  OerPreparePacketHttpMessageConverter oerPreparePacketHttpMessageConverter() {
    return new OerPreparePacketHttpMessageConverter(ilpCodecContext);
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
    converters.add(oerPreparePacketHttpMessageConverter());
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.replaceAll(messageConverter -> {
      if (messageConverter instanceof MappingJackson2HttpMessageConverter) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
        //return messageConverter;
      } else {
        return messageConverter;
      }
    });
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(rateLimitSettingsConverter());
    registry.addConverter(accountBalanceSettingsConverter());
    registry.addConverter(accountSettingsConverter());
  }
}
