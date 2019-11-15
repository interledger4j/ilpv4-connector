package org.interledger.connector.server.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Optional;

/**
 * Internally for testing the connector from spring boot tests. Note that all methods require
 * a URI to be passed in. This is because we start the server on a random port and the random port is not assigned
 * until after the context has been loaded. This makes it impossible to configure the local server URL
 * using something like
 * <pre>
 *   @FeignClient(url="http://localhost:${server.port}")
 * </pre>
 * Hence the reason that the url property is initialzed to a placeholder value.
 */
@FeignClient(name = "connector-admin-client", url = "http://placeholder", decode404 = true,
  configuration = ConnectorAdminClient.ConnectorAdminClientConfiguration.class)
public interface ConnectorAdminClient {

  @PostMapping(value = "/accounts", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  ResponseEntity<ImmutableAccountSettings> createAccount(URI baseURL, @RequestBody AccountSettings accountSettings);

  @GetMapping(value = "/accounts/{id}", produces = APPLICATION_JSON_VALUE)
  Optional<ImmutableAccountSettings> findAccount(URI baseURL, @RequestParam("id") String accountId);


  class ConnectorAdminClientConfiguration {
    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(
      @Value("${interledger.connector.adminPassword}") String adminPassword) {
      return new BasicAuthRequestInterceptor("admin", adminPassword);
    }
  }

}