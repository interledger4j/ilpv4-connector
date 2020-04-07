package org.interledger.connector.opay.config;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import org.interledger.connector.opay.controllers.converters.InvoiceIdConverter;
import org.interledger.connector.opay.jackson.ObjectMapperFactory;
import org.interledger.connector.opay.model.OpenPaymentsMetadata;
import org.interledger.connector.opay.service.InvoiceService;
import org.interledger.connector.opay.service.ilp.IlpInvoiceServiceImpl;
import org.interledger.connector.opay.service.ilp.OpaStreamConnectionGenerator;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;
import org.interledger.stream.receiver.StreamReceiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.List;
import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(OpenPaymentsMetadataFromPropertyFile.class)
@ComponentScan(basePackages = {"org.interledger.connector.opay"})
public class OpenPaymentsConfig implements WebMvcConfigurer {

  public static final String OPEN_PAYMENTS = "OPEN_PAYMENTS";

  @Autowired
  @Qualifier(OPEN_PAYMENTS)
  private ObjectMapper objectMapper;

  @Autowired
  private ApplicationContext applicationContext;

  @Bean
  public Supplier<OpenPaymentsMetadata> openPaymentsMetadataSupplier() {
    return () -> applicationContext.getBean(OpenPaymentsMetadata.class);
  }

  /**
   * This module has its own ObjectMapper bean so that it can be used as a standalone jar without
   * and extra configuration.
   */
  @Bean
  @Qualifier(OPEN_PAYMENTS)
  public ObjectMapper openPaymentsObjectMapper() {
    return ObjectMapperFactory.create();
  }

  @Bean
  @Qualifier(OPEN_PAYMENTS)
  public InvoiceService ilpInvoiceService() {
    return new IlpInvoiceServiceImpl();
  }

  @Bean
  @Qualifier(OPEN_PAYMENTS)
  public StreamConnectionGenerator streamConnectionGenerator() {
    return new OpaStreamConnectionGenerator();
  }

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
