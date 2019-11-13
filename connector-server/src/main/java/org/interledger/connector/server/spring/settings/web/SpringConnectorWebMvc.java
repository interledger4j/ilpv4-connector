package org.interledger.connector.server.spring.settings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.interledger.connector.persistence.converters.AccountBalanceSettingsEntityConverter;
import org.interledger.connector.persistence.converters.AccountSettingsEntityConverter;
import org.interledger.connector.persistence.converters.FxRateOverridesEntityConverter;
import org.interledger.connector.persistence.converters.RateLimitSettingsEntityConverter;
import org.interledger.connector.persistence.converters.SettlementEngineDetailsEntityConverter;
import org.interledger.connector.server.spring.controllers.converters.OerPreparePacketHttpMessageConverter;
import org.interledger.connector.server.spring.settings.CodecContextConfig;
import org.interledger.connector.server.spring.settings.link.IlpOverHttpConfig;
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
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.interledger.connector.core.ConfigConstants.ILP_OVER_HTTP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

/**
 * Web config for the Spring Connector.
 */
@Configuration
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = ILP_OVER_HTTP_ENABLED, havingValue = "true")
@EnableWebMvc
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL})
@ComponentScan(basePackages = "org.interledger.connector.server.spring.controllers")
@Import({IlpOverHttpConfig.class, JacksonConfig.class, SecurityConfiguration.class})
public class SpringConnectorWebMvc implements WebMvcConfigurer {

  // TODO: Configure TLS
  // TODO: Configure HTTP/2
  @Autowired
  @Qualifier(CodecContextConfig.ILP)
  private CodecContext ilpCodecContext;

  @Autowired
  private ObjectMapper objectMapper;

  ////////////////////////
  // SpringConverters
  ////////////////////////

  @Autowired
  private RateLimitSettingsEntityConverter rateLimitSettingsEntityConverter;

  @Autowired
  private AccountBalanceSettingsEntityConverter accountBalanceSettingsEntityConverter;

  @Autowired
  private SettlementEngineDetailsEntityConverter settlementEngineDetailsEntityConverter;

  @Autowired
  private AccountSettingsEntityConverter accountSettingsConverter;

  @Autowired
  private FxRateOverridesEntityConverter fxRateOverrideEntityConverter;

  ////////////////////////
  // HttpMessageConverters
  ////////////////////////

  @Bean
  OerPreparePacketHttpMessageConverter oerPreparePacketHttpMessageConverter() {
    return new OerPreparePacketHttpMessageConverter(ilpCodecContext);
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // For any byte[] payloads (e.g., `/settlements`)
    ByteArrayHttpMessageConverter octetStreamConverter = new ByteArrayHttpMessageConverter();
    octetStreamConverter.setSupportedMediaTypes(Lists.newArrayList(APPLICATION_OCTET_STREAM));
    converters.add(octetStreamConverter);

    converters.add(new MappingJackson2HttpMessageConverter(objectMapper)); // For any JSON payloads.
    converters.add(oerPreparePacketHttpMessageConverter());
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.replaceAll(messageConverter -> {
      if (messageConverter instanceof MappingJackson2HttpMessageConverter) {
        // Necessary to make sure the correct ObjectMapper is used in all Jackson Message Converters.
        return new MappingJackson2HttpMessageConverter(objectMapper);
      } else {
        return messageConverter;
      }
    });
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(rateLimitSettingsEntityConverter);
    registry.addConverter(accountBalanceSettingsEntityConverter);
    registry.addConverter(settlementEngineDetailsEntityConverter);
    registry.addConverter(accountSettingsConverter);
    registry.addConverter(fxRateOverrideEntityConverter);
  }
}
