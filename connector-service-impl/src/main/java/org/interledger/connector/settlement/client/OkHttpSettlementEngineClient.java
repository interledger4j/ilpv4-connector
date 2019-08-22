package org.interledger.connector.settlement.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.connector.settlement.SettlementEngineClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.settlement.SettlementEngineClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Objects;
import java.util.Optional;

import static org.interledger.connector.settlement.SettlementConstants.ACCOUNTS;
import static org.interledger.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
import static org.interledger.connector.settlement.SettlementConstants.MESSAGES;
import static org.interledger.connector.settlement.SettlementConstants.SETTLEMENTS;

/**
 * The default implementation of {@link OkHttpSettlementEngineClient}.
 */
public class OkHttpSettlementEngineClient implements SettlementEngineClient {

  private static final okhttp3.MediaType APPLICATION_JSON = okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE);
  private static final okhttp3.MediaType APPLICATION_OCTET_STREAM =
    okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ObjectMapper objectMapper;
  private final OkHttpClient okHttpClient;

  /**
   * Required-args Constructor.
   *
   * @param okHttpClient An {@link OkHttpClient} to
   * @param objectMapper
   */
  public OkHttpSettlementEngineClient(
    final OkHttpClient okHttpClient, final ObjectMapper objectMapper
  ) {
    this.okHttpClient = Objects.requireNonNull(okHttpClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @Override
  public CreateSettlementAccountResponse createSettlementAccount(
    final AccountId accountId,
    final HttpUrl settlementEngineBaseUrl,
    final CreateSettlementAccountRequest createSettlementAccountRequest
  ) {
    HttpUrl requestUrl = settlementEngineBaseUrl.newBuilder()
      .addPathSegment(ACCOUNTS)
      .build();

    final Request okHttpRequest;
    try {
      okHttpRequest = new Request.Builder()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .url(requestUrl)
        .post(RequestBody.create(
          objectMapper.writeValueAsString(createSettlementAccountRequest),
          APPLICATION_JSON
        ))
        .build();
    } catch (Exception e) {
      throw new SettlementEngineClientException(e.getMessage(), e, accountId, Optional.empty());
    }

    try (Response okHttpResponse = okHttpClient.newCall(okHttpRequest).execute()) {
      if (!okHttpResponse.isSuccessful()) {
        final String errorMessage = String.format("Unable to create account in settlement engine. " +
            "initiateSettlementRequest=%s okHttpRequest=%s okHttpResponse=%s",
          createSettlementAccountRequest, okHttpRequest, okHttpResponse
        );
        throw new SettlementEngineClientException(errorMessage, accountId, Optional.empty());
      }

      // Marshal the okHttpResponse to the correct object.
      final CreateSettlementAccountResponse createSettlementAccountResponse =
        objectMapper.readValue(okHttpResponse.body().charStream(), CreateSettlementAccountResponse.class);

      logger.trace("Settlement account created successfully. createSettlementAccountResponse={} okHttpRequest={} " +
          "okHttpResponse={} settlementResponse={}",
        createSettlementAccountRequest,
        okHttpRequest,
        okHttpResponse,
        createSettlementAccountResponse
      );

      return createSettlementAccountResponse;

    } catch (Exception e) {
      throw new SettlementEngineClientException(e.getMessage(), e, accountId, Optional.empty());
    }
  }

  @Override
  public InitiateSettlementResponse initiateSettlement(
    final AccountId accountId,
    final SettlementEngineAccountId settlementEngineAccountId,
    final String idempotencyKey,
    final HttpUrl settlementEngineBaseUrl,
    final InitiateSettlementRequest initiateSettlementRequest
  ) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(idempotencyKey);
    Objects.requireNonNull(initiateSettlementRequest);

    final HttpUrl requestUrl = settlementEngineBaseUrl.newBuilder()
      .addPathSegment(ACCOUNTS)
      .addEncodedPathSegment(settlementEngineAccountId.value())
      .addPathSegment(SETTLEMENTS)
      .build();

    final Request okHttpRequest;
    try {
      okHttpRequest = new Request.Builder()
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(IDEMPOTENCY_KEY, idempotencyKey)
        .url(requestUrl)
        .post(RequestBody.create(
          objectMapper.writeValueAsString(initiateSettlementRequest),
          APPLICATION_JSON
        ))
        .build();
    } catch (Exception e) {
      throw new SettlementEngineClientException(
        e.getMessage(), e, accountId, Optional.ofNullable(settlementEngineAccountId)
      );
    }

    logger.trace("Instructing settlement engine to settle. okHttpRequest={}", okHttpRequest);

    try (Response okHttpResponse = okHttpClient.newCall(okHttpRequest).execute()) {
      if (!okHttpResponse.isSuccessful()) {
        final String errorMessage = String.format("Unable to initiate settlement. " +
            "initiateSettlementRequest=%s okHttpRequest=%s okHttpResponse=%s",
          initiateSettlementRequest, okHttpRequest, okHttpResponse
        );
        throw new SettlementEngineClientException(
          errorMessage, accountId, Optional.ofNullable(settlementEngineAccountId)
        );
      }

      // Marshal the okHttpResponse to the correct object.
      final InitiateSettlementResponse initiateSettlementResponse =
        objectMapper.readValue(okHttpResponse.body().byteStream(), InitiateSettlementResponse.class);

      logger.trace("Settlement initiated successfully. " +
          "initiateSettlementRequest={} okHttpRequest={} okHttpResponse={} initiateSettlementResponse={}",
        initiateSettlementRequest,
        okHttpRequest,
        okHttpResponse,
        initiateSettlementResponse
      );

      return initiateSettlementResponse;
    } catch (Exception e) {
      throw new SettlementEngineClientException(
        e.getMessage(), e, accountId, Optional.ofNullable(settlementEngineAccountId)
      );
    }
  }

  @Override
  public SendMessageResponse sendMessageFromPeer(
    final AccountId accountId,
    final SettlementEngineAccountId settlementEngineAccountId,
    final HttpUrl settlementEngineBaseUrl,
    final SendMessageRequest sendMessageRequest
  ) {

    Objects.requireNonNull(accountId);
    Objects.requireNonNull(sendMessageRequest);

    final HttpUrl requestUrl = settlementEngineBaseUrl.newBuilder()
      .addPathSegment(ACCOUNTS)
      .addEncodedPathSegment(settlementEngineAccountId.value())
      .addPathSegment(MESSAGES)
      .build();

    Request okHttpRequest;
    try {
      okHttpRequest = new Request.Builder()
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .url(requestUrl)
        // Proxy the bytes as-is. These are opaque at this level of code (in the connector).
        .post(RequestBody.create(sendMessageRequest.data(), APPLICATION_OCTET_STREAM))
        .build();

    } catch (Exception e) {
      throw new SettlementEngineClientException(
        e.getMessage(), e, accountId, Optional.ofNullable(settlementEngineAccountId)
      );
    }

    logger.trace("Sending message to settlement engine. settlementRequest={} settlementEngineUrl={}",
      sendMessageRequest, requestUrl
    );

    try (Response okHttpResponse = okHttpClient.newCall(okHttpRequest).execute()) {
      if (!okHttpResponse.isSuccessful()) {
        final String errorMessage = String.format("Unable to send message to settlement engine. " +
            "initiateSettlementRequest=%s okHttpRequest=%s okHttpResponse=%s",
          sendMessageRequest, okHttpRequest, okHttpResponse
        );
        throw new SettlementEngineClientException(
          errorMessage, accountId, Optional.ofNullable(settlementEngineAccountId)
        );
      }
      // Marshal the okHttpResponse to the correct object.
      final SendMessageResponse sendMessageResponse = SendMessageResponse.builder()
        .data(okHttpResponse.body().bytes())
        .build();

      logger.trace("Settlement message sent successfully. createSettlementAccountResponse={} okHttpRequest={} " +
          "okHttpResponse={} settlementResponse={}",
        sendMessageRequest,
        okHttpRequest,
        okHttpResponse,
        sendMessageResponse
      );

      return sendMessageResponse;
    } catch (Exception e) {
      throw new SettlementEngineClientException(
        e.getMessage(), e, accountId, Optional.ofNullable(settlementEngineAccountId)
      );
    }
  }
}
