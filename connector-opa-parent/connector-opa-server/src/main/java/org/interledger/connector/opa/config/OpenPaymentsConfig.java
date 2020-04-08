package org.interledger.connector.opa.config;

import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.config.settings.OpenPaymentsMetadataFromPropertyFile;
import org.interledger.connector.opa.config.settings.OpenPaymentsSettings;
import org.interledger.connector.opa.config.settings.OpenPaymentsSettingsFromPropertyFile;
import org.interledger.connector.opa.controllers.converters.InvoiceIdConverter;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.service.InvoiceService;
import org.interledger.connector.opa.service.ilp.IlpInvoiceService;
import org.interledger.connector.opa.service.ilp.OpaStreamConnectionGenerator;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
@EnableConfigurationProperties(OpenPaymentsSettingsFromPropertyFile.class)
@ComponentScan(basePackages = {"org.interledger.connector.opa"})
public class OpenPaymentsConfig implements WebMvcConfigurer {

  public static final String OPEN_PAYMENTS = "OPEN_PAYMENTS";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ApplicationContext applicationContext;

  @Bean
  public Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier() {
    return () -> applicationContext.getBean(OpenPaymentsSettings.class);
  }

  @Bean
  @Qualifier(OPEN_PAYMENTS)
  public InvoiceService ilpInvoiceService(
    Supplier<ConnectorSettings> connectorSettingsSupplier,
    PaymentPointerResolver paymentPointerResolver,
    @Value("${" + SPSP__URL_PATH + ":}") final String opaUrlPath
  ) {
    return new IlpInvoiceService(connectorSettingsSupplier, paymentPointerResolver, opaUrlPath);
  }

  @Bean
  @Qualifier(OPEN_PAYMENTS)
  public StreamConnectionGenerator opaStreamConnectionGenerator() {
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
