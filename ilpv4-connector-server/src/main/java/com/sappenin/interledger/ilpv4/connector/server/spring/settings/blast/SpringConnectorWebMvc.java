package com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast;

import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.server.spring.converters.OerPreparePacketHttpMessageConverter;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.interledger.encoding.asn.framework.CodecContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;

/**
 * Web config for the Spring Connector.
 */
@Configuration
@ConditionalOnProperty(ConnectorProperties.BLAST_ENABLED)
@EnableWebMvc
@ComponentScan(basePackageClasses = IlpHttpController.class)
@Import({JacksonConfig.class, SecurityConfiguration.class})
public class SpringConnectorWebMvc implements WebMvcConfigurer {

  // TODO: Configure TLS
  // TODO: Configure HTTP/2

  @Autowired
  @Qualifier(ILP)
  private CodecContext ilpCodecContext;

  @Bean
  OerPreparePacketHttpMessageConverter oerPreparePacketHttpMessageConverter() {
    return new OerPreparePacketHttpMessageConverter(ilpCodecContext);
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    //converters.add(ProblemConver)
    converters.add(oerPreparePacketHttpMessageConverter());
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.replaceAll(messageConverter -> {
      if (messageConverter instanceof MappingJackson2HttpMessageConverter) {
        //return new MappingJackson2HttpMessageConverter(objectMapper);
        return messageConverter;
      } else {
        return messageConverter;
      }
    });
  }
}