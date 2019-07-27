package org.interledger.ilpv4.connector.settlement;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClientException;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.MESSAGES;
import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.SETTLEMENTS;

/**
 * The default implementation of {@link DefaultSettlementEngineClient}.
 */
public class DefaultSettlementEngineClient implements SettlementEngineClient {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final RestTemplate restTemplate;
  private final HttpUrl settlementEngineBaseUrl;

  /**
   * Required-args Constructor.
   *
   * @param restTemplate
   * @param settlementEngineBaseUrl A fully-qualified URL that can be used to reach a settlement engine.
   */
  public DefaultSettlementEngineClient(final RestTemplate restTemplate, final HttpUrl settlementEngineBaseUrl) {
    this.restTemplate = Objects.requireNonNull(restTemplate);
    this.settlementEngineBaseUrl = Objects.requireNonNull(settlementEngineBaseUrl);
  }

  @Override
  public void createSettlementAccount(final AccountId accountId, String settlementEngineAccountId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM));

      final HttpEntity<?> entity = new HttpEntity<>(HttpEntity.EMPTY, headers);
      final URI endpointUrl = settlementEngineBaseUrl.newBuilder()
        .addPathSegment(ACCOUNTS)
        .addEncodedPathSegment(settlementEngineAccountId)
        .build().uri();
      final ResponseEntity<Resource> response = restTemplate.postForEntity(endpointUrl, entity, Resource.class);

      // TODO: Convert this to a Problem once the SE RFC conforms to Problems RFC.
      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new SettlementEngineClientException(
          String.format("Unable to create new account in Settlement Engine: %s", settlementEngineAccountId),
          accountId);
      }

    } catch (Exception e) {
      throw new SettlementEngineClientException(e.getMessage(), e, accountId);
    }
  }

  @Override
  public ResponseEntity<Resource> initiateSettlement(
    final AccountId accountId, final UUID idempotencyKey, final SettlementQuantity settlementQuantity
  ) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(idempotencyKey);
    Objects.requireNonNull(settlementQuantity);

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM));
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.set(IDEMPOTENCY_KEY, idempotencyKey.toString());

      // TODO: Make the body of this request a JSON object for extensibility...
      final HttpEntity<String> entity = new HttpEntity<>(settlementQuantity.amount() + "", headers);
      final URI settlementEngineSettlementUrl = settlementEngineBaseUrl.newBuilder()
        .addPathSegment(ACCOUNTS)
        .addEncodedPathSegment(accountId.value())
        .addPathSegment(SETTLEMENTS)
        .build().uri();

      logger.trace("Sending settlement of amount {} to settlement engine: {}", settlementQuantity.amount(),
        settlementEngineSettlementUrl);

      final ResponseEntity<Resource> response =
        restTemplate.exchange(settlementEngineSettlementUrl, HttpMethod.POST, entity, Resource.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        logger
          .trace("Sent settlement of {} to settlement engine: {}", settlementQuantity, settlementEngineSettlementUrl);
        return response;
      } else {
        logger
          .error("Error sending settlement. Settlement engine responded with HTTP code: {}", response.getStatusCode());
        return response;
      }
    } catch (Exception e) {
      throw new SettlementEngineClientException(e.getMessage(), e, accountId);
    }
  }

  /**
   * Given a message from a Peer's settlement engine, forward it to the local settlement engine by making an HTTP Post
   * request to `{settlementEngineBaseUrl}/{accounId}/message`, and then return the response.
   *
   * @param accountId
   * @param data      A byte array of data that two Settlement Engines can understand, but for which this client views
   *                  as opaque data.
   *
   * @return An opaque byte array response destined for the peer's settlment engine.
   */
  @Override
  public byte[] sendMessageFromPeer(final AccountId accountId, final byte[] data) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(data);

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM));

      final HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);
      final URI seMessageUrl = settlementEngineBaseUrl.newBuilder()
        .addPathSegment(ACCOUNTS)
        .addEncodedPathSegment(accountId.value())
        .addPathSegment(MESSAGES)
        .build().uri();
      final ResponseEntity<Resource> response =
        restTemplate.exchange(seMessageUrl, HttpMethod.POST, entity, Resource.class);

      return ByteStreams.toByteArray(response.getBody().getInputStream());
    } catch (Exception e) {
      throw new SettlementEngineClientException(e.getMessage(), e, accountId);
    }
  }
}
