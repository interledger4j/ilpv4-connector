package org.interledger.connector.client;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.payments.ListStreamPaymentsResponse;
import org.interledger.connector.routing.StaticRoute;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import okhttp3.HttpUrl;
import org.zalando.problem.ThrowableProblem;

import java.util.Objects;
import java.util.Optional;

/**
 * A feign HTTP client for interacting with the Connector's Admin API.
 */
public interface ConnectorAdminClient {

  String ACCEPT = "Accept:";
  String CONTENT_TYPE = "Content-Type:";

  String ID = "id";
  String PREFIX = "prefix";
  String APPLICATION_JSON = "application/json";
  String PLAIN_TEXT = "text/plain";

  /**
   * Static constructor to build a new instance of this Connector Admin client.
   *
   * @param httpUrl                     The {@link HttpUrl} of the Connector.
   * @param basicAuthRequestInterceptor A {@link RequestInterceptor} that injects the HTTP Basic auth credentials into
   *                                    each request.
   *
   * @return A {@link ConnectorAdminClient}.
   */
  static ConnectorAdminClient construct(
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
      .target(ConnectorAdminClient.class, httpUrl.toString());
  }

  @RequestLine("POST /accounts")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  AccountSettings createAccount(AccountSettings accountSettings) throws ThrowableProblem;

  /**
   * Exists only for testing so that the {@link Response} can be accessed.
   *
   * @param accountSettings An {@link AccountSettings} that can be used to create an account.
   *
   * @return An {@link AccountSettings}.
   */
  @RequestLine("POST /accounts")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Response createAccountAsResponse(AccountSettings accountSettings) throws ThrowableProblem;

  @RequestLine("PUT /accounts/{id}")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  AccountSettings updateAccount(@Param(ID) String accountId, AccountSettings accountSettings)
    throws ThrowableProblem;

  @RequestLine("GET /accounts/{id}")
  @Headers( {
    ACCEPT + APPLICATION_JSON
  })
  Optional<AccountSettings> findAccount(@Param(ID) String accountId) throws ThrowableProblem;

  @RequestLine("GET /accounts/{id}/payments")
  @Headers( {
    ACCEPT + APPLICATION_JSON
  })
  ListStreamPaymentsResponse findAccountPayments(@Param(ID) String accountId) throws ThrowableProblem;

  @RequestLine("DELETE /accounts/{id}")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  void deleteAccount(@Param(ID) String accountId) throws ThrowableProblem;

  @RequestLine("DELETE /accounts/{id}")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  Response deleteAccountAsResponse(@Param(ID) String accountId) throws ThrowableProblem;

  @RequestLine("PUT /routes/static/{prefix}")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + APPLICATION_JSON
  })
  StaticRoute createStaticRoute(@Param(PREFIX) String prefix, StaticRoute route) throws ThrowableProblem;

  @RequestLine("POST /encryption/encrypt")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    CONTENT_TYPE + PLAIN_TEXT
  })
  String encrypt(String message) throws ThrowableProblem;

  @RequestLine("POST /encryption/refresh")
  @Headers( {
    ACCEPT + APPLICATION_JSON
  })
  long refresh() throws ThrowableProblem;

}
