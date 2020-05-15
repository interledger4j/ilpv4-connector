package org.interledger.connector.server.client;

import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS_PAYMENTS_PATH;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.balances.AccountBalanceResponse;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.server.spring.controllers.pay.ListStreamPaymentsResponse;
import org.interledger.connector.server.spring.controllers.pay.LocalSendPaymentRequest;
import org.interledger.connector.server.spring.controllers.pay.LocalSendPaymentResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import okhttp3.HttpUrl;
import org.zalando.problem.ThrowableProblem;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public interface ConnectorUserClient {

  String ACCEPT = "Accept:";
  String CONTENT_TYPE = "Content-Type:";
  String APPLICATION_JSON = "application/json";
  String ACCEPT_JSON = ACCEPT + APPLICATION_JSON;
  String AUTHORIZATION = "Authorization: {auth}";
  String CONTENT_TYPE_JSON = CONTENT_TYPE + APPLICATION_JSON;

  /**
   * Static constructor to build a new instance of this Connector Admin client.
   *
   * @param httpUrl The {@link HttpUrl} of the Connector.
   * @return A {@link ConnectorAdminClient}.
   */
  static ConnectorUserClient construct(
    final HttpUrl httpUrl) {
    Objects.requireNonNull(httpUrl);

    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(ConnectorUserClient.class, httpUrl.toString());
  }

  @RequestLine("GET /accounts/{accountId}/balance")
  @Headers( {AUTHORIZATION, ACCEPT_JSON})
  AccountBalanceResponse getBalance(@Param("auth") String authorizationHeader,
                                    @Param("accountId") String accountId);

  @RequestLine("GET /accounts/{accountId}/tokens")
  @Headers( {AUTHORIZATION, ACCEPT_JSON})
  List<AccessToken> getTokens(@Param("auth") String authorizationHeader,
                              @Param("accountId") AccountId accountId) throws ThrowableProblem;

  @RequestLine("POST /accounts/{accountId}/tokens")
  @Headers( {AUTHORIZATION, ACCEPT_JSON, CONTENT_TYPE_JSON})
  AccessToken createToken(@Param("auth") String authorizationHeader,
                          @Param("accountId") AccountId accountId) throws ThrowableProblem;


  @RequestLine("DELETE /accounts/{accountId}/tokens")
  @Headers( {AUTHORIZATION, ACCEPT_JSON, CONTENT_TYPE_JSON})
  void deleteTokens(@Param("auth") String authorizationHeader,
                    @Param("accountId") AccountId accountId) throws ThrowableProblem;

  @RequestLine("DELETE /accounts/{accountId}/tokens/{tokenId}")
  @Headers( {AUTHORIZATION, ACCEPT_JSON, CONTENT_TYPE_JSON})
  void deleteToken(@Param("auth") String authorizationHeader,
                   @Param("accountId") AccountId accountId,
                   @Param("tokenId") long tokenId) throws ThrowableProblem;

  @RequestLine("GET /accounts/{accountId}/payments/{paymentId}")
  @Headers( {AUTHORIZATION, ACCEPT_JSON})
  Optional<StreamPayment> findById(@Param("auth") String authorizationHeader,
                                   @Param("accountId") AccountId accountId,
                                   @Param("paymentId") String paymentId) throws ThrowableProblem;

  @RequestLine("POST /accounts/{accountId}/payments")
  @Headers( {AUTHORIZATION, ACCEPT_JSON, CONTENT_TYPE_JSON})
  LocalSendPaymentResponse sendPayment(@Param("auth") String authorizationHeader,
                                       @Param("accountId") AccountId accountId,
                                       LocalSendPaymentRequest request) throws ThrowableProblem;

  @RequestLine("GET " + SLASH_ACCOUNTS_PAYMENTS_PATH)
  @Headers( {AUTHORIZATION, ACCEPT_JSON, CONTENT_TYPE_JSON})
  ListStreamPaymentsResponse listPayments(@Param("auth") String authorizationHeader,
                                          @Param("accountId") AccountId accountId) throws ThrowableProblem;

  @RequestLine("GET " + SLASH_ACCOUNTS_PAYMENTS_PATH + "?page={page}&")
  @Headers( {AUTHORIZATION, ACCEPT_JSON, CONTENT_TYPE_JSON})
  ListStreamPaymentsResponse listPayments(@Param("auth") String authorizationHeader,
                                          @Param("accountId") AccountId accountId,
                                          @QueryMap Map<String, Object> queryMap) throws ThrowableProblem;


}