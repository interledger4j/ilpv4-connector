package org.interledger.connector.opa.controllers;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.controllers.converters.InvoiceIdConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.List;

@Configuration
public class OpenPaymentsTestMvcConfig implements WebMvcConfigurer {
  @Autowired
  ObjectMapper objectMapper;

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new InvoiceIdConverter());
  }

  /**
   * We do not need this when using the Open Payments module in connector-server, as that
   * module is already configured like this.  However, if we want this module to work out of the box,
   * we should register message converters.
   */
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // For any byte[] payloads (e.g., `/settlements`)
    ByteArrayHttpMessageConverter octetStreamConverter = new ByteArrayHttpMessageConverter();
    octetStreamConverter.setSupportedMediaTypes(Lists.newArrayList(APPLICATION_OCTET_STREAM));
    converters.add(octetStreamConverter);

    converters.add(constructProblemsJsonConverter()); // For ProblemsJson only.
    converters.add(new MappingJackson2HttpMessageConverter(objectMapper)); // For any JSON payloads.
    converters.add(new StringHttpMessageConverter()); // for text/plain
  }

  @VisibleForTesting
  protected MappingJackson2HttpMessageConverter constructProblemsJsonConverter() {
    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    final MappingJackson2HttpMessageConverter problemsJsonConverter
      = new MappingJackson2HttpMessageConverter(objectMapper);
    problemsJsonConverter.setSupportedMediaTypes(Lists.newArrayList(MediaTypes.PROBLEM, MediaTypes.X_PROBLEM));

    return problemsJsonConverter;
  }
}
