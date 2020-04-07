package org.interledger.connector.opa.controllers;

import org.interledger.connector.opa.config.OpenPaymentsConfig;
import org.interledger.connector.opa.config.OpenPaymentsMetadataFromPropertyFile;

import com.google.common.collect.Lists;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

  @EnableConfigurationProperties(OpenPaymentsMetadataFromPropertyFile.class)
  @EnableWebMvc
  @Import(OpenPaymentsConfig.class)
  public static class TestConfiguration {

  }
}
