package org.interledger.connector.server.client;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import feign.Feign;
import feign.Headers;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import okhttp3.HttpUrl;
import org.zalando.problem.ThrowableProblem;

import java.util.Objects;

/**
 * Internally for testing the connector from spring boot tests.
 */
@VisibleForTesting
public interface ConnectorAdminTestClient extends ConnectorAdminClient {

  /**
   * Static constructor to build a new instance of this Connector Admin client.
   *
   * @param httpUrl                     The {@link HttpUrl} of the Connector.
   * @param basicAuthRequestInterceptor A {@link RequestInterceptor} that injects the HTTP Basic auth credentials into
   *                                    each request.
   *
   * @return A {@link ConnectorAdminClient}.
   */
  static ConnectorAdminTestClient construct(
    final HttpUrl httpUrl, final RequestInterceptor basicAuthRequestInterceptor
  ) {
    Objects.requireNonNull(httpUrl);
    Objects.requireNonNull(basicAuthRequestInterceptor);

    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .requestInterceptor(basicAuthRequestInterceptor)
      .target(ConnectorAdminTestClient.class, httpUrl.toString());
  }

  /**
   * Exists only for testing.
   *
   * @param accountSettingsAsJson A JSON string that can be used to test invalid inputs.
   *
   * @return A {@link Response}.
   */
  @RequestLine("POST /accounts")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  AccountSettings createAccountUsingJson(String accountSettingsAsJson) throws ThrowableProblem;

}
