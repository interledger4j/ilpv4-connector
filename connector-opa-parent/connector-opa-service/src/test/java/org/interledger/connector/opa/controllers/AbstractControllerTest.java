package org.interledger.connector.opa.controllers;


import static org.interledger.connector.opa.controllers.InvoicesController.OPEN_PAYMENTS;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.config.OpenPaymentsSettings;
import org.interledger.connector.opa.config.OpenPaymentsSettingsFromPropertyFile;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.SupportedAssets;
import org.interledger.connector.opa.service.InvoiceService;
import org.interledger.core.InterledgerAddress;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.function.Supplier;

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
  @Qualifier(OPEN_PAYMENTS)
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

//  @EnableConfigurationProperties(OpenPaymentsSettingsFromPropertyFile.class)
  @EnableWebMvc
  @ComponentScan(basePackages = "org.interledger.connector.opa")
  public static class TestConfiguration implements WebMvcConfigurer {
    @Bean
    public ObjectMapper objectMapper() {
      return ObjectMapperFactory.create();
    }

    @Bean
    public Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier() {
      return () -> OpenPaymentsSettings.builder()
        .ilpOperatorAddress(InterledgerAddress.of("test.jc1"))
        .metadata(OpenPaymentsMetadata.builder()
          .issuer(HttpUrl.parse("https://wallet.example.com"))
          .authorizationIssuer(HttpUrl.parse("https://auth.wallet.example.com"))
          .addAssetsSupported(SupportedAssets.XRP, SupportedAssets.USD)
          .build()
        )
        .build();
    }
  }
}
