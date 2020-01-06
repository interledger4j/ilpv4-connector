package org.interledger.connector.server.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.balances.AccountBalanceResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

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
@FeignClient(name = "connector-user-client", url = "http://placeholder", decode404 = true)
public interface ConnectorUserClient {

  @GetMapping(value = "/accounts/{id}/balance", produces = APPLICATION_JSON_VALUE)
  AccountBalanceResponse getBalance(URI baseURL,
                                    @RequestHeader(value = "Authorization") String authorizationHeader,
                                    @PathVariable("id") String accountId);

}