package org.interledger.connector.opa.controllers;

import static org.interledger.connector.opa.config.OpenPaymentsConfig.OPEN_PAYMENTS;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.config.OpenPaymentsConfig;
import org.interledger.connector.opa.config.settings.OpenPaymentsMetadataFromPropertyFile;
import org.interledger.connector.opa.config.settings.OpenPaymentsSettingsFromPropertyFile;
import org.interledger.connector.opa.service.InvoiceService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ContextConfiguration(classes = {
  AbstractControllerTest.TestConfiguration.class
})
@ActiveProfiles({"test"})
public class AbstractControllerTest {

  @MockBean
  InvoiceService invoiceServiceMock;

  @MockBean
  @Qualifier(OPEN_PAYMENTS)
  StreamConnectionGenerator streamConnectionGeneratorMock;

  @MockBean
  ServerSecretSupplier serverSecretSupplierMock;

  /**
   * Construct an instance of {@link HttpHeaders} that contains everything needed to make a valid request to the
   * `/settlements` API endpoint on a Connector for a JSON payload with no idempotence header.
   *
   * @return An instance of {@link HttpHeaders}.
   */
  protected HttpHeaders testJsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @EnableConfigurationProperties(OpenPaymentsSettingsFromPropertyFile.class)
  @EnableWebMvc
  @Import(OpenPaymentsConfig.class)
  public static class TestConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
      return ObjectMapperFactory.create();
    }
  }
}
