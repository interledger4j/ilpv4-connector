package org.interledger.ilpv4.connector.settlement.client;

/**
 * The default implementation of {@link RestTemplateSettlementEngineClient}.
 *
 * @see "https://github.com/sappenin/java-ilpv4-connector/issues/235"
 * @deprecated Will go away in-favor of {@link OkHttpSettlementEngineClient}.
 */
@Deprecated
public class RestTemplateSettlementEngineClient {// { implements SettlementEngineClient {

  //  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  //
  //  private final RestTemplate restTemplate;
  //  private final HttpUrl settlementEngineBaseUrl;
  //
  //  /**
  //   * Required-args Constructor.
  //   *
  //   * @param restTemplate
  //   * @param settlementEngineBaseUrl A fully-qualified URL that can be used to reach a settlement engine.
  //   */
  //  public RestTemplateSettlementEngineClient(final RestTemplate restTemplate, final HttpUrl settlementEngineBaseUrl) {
  //    this.restTemplate = Objects.requireNonNull(restTemplate);
  //    this.settlementEngineBaseUrl = Objects.requireNonNull(settlementEngineBaseUrl);
  //  }
  //
  //  @Override
  //  public void createSettlementAccount(final AccountId accountId, String settlementEngineAccountId) {
  //    try {
  //      HttpHeaders headers = new HttpHeaders();
  //      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
  //      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM));
  //
  //      final HttpEntity<?> entity = new HttpEntity<>(HttpEntity.EMPTY, headers);
  //      final URI endpointUrl = settlementEngineBaseUrl.newBuilder()
  //        .addPathSegment(ACCOUNTS)
  //        .addEncodedPathSegment(settlementEngineAccountId)
  //        .build().uri();
  //      final ResponseEntity<Resource> response = restTemplate.postForEntity(endpointUrl, entity, Resource.class);
  //
  //      // TODO: Convert this to a Problem once the SE RFC conforms to Problems RFC.
  //      if (!response.getStatusCode().is2xxSuccessful()) {
  //        throw new SettlementEngineClientException(
  //          String.format("Unable to create new account in Settlement Engine: %s", settlementEngineAccountId),
  //          accountId);
  //      }
  //
  //    } catch (Exception e) {
  //      throw new SettlementEngineClientException(e.getMessage(), e, accountId);
  //    }
  //  }
  //
  //  @Override
  //  public ResponseEntity<Resource> initiateSettlement(
  //    final AccountId accountId, final UUID idempotencyKey, final InitiateSettlementRequest initiateSettlementRequest
  //  ) {
  //    Objects.requireNonNull(accountId);
  //    Objects.requireNonNull(idempotencyKey);
  //    Objects.requireNonNull(initiateSettlementRequest);
  //
  //    try {
  //      HttpHeaders headers = new HttpHeaders();
  //      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
  //      headers.setContentType(MediaType.APPLICATION_JSON);
  //      headers.set(IDEMPOTENCY_KEY, idempotencyKey.toString());
  //
  //      final HttpEntity<InitiateSettlementRequest> entity = new HttpEntity<>(initiateSettlementRequest, headers);
  //      final URI settlementEngineSettlementUrl = settlementEngineBaseUrl.newBuilder()
  //        .addPathSegment(ACCOUNTS)
  //        .addEncodedPathSegment(accountId.value())
  //        .addPathSegment(SETTLEMENTS)
  //        .build().uri();
  //
  //      logger.trace("Instructing Settlement Engine to settle. settlementRequest={} settlementEngineUrl={}",
  //        initiateSettlementRequest,
  //        settlementEngineSettlementUrl
  //      );
  //
  //      // TODO: Once the implementation starts returning an object payload, update this to use a SettlementResponse
  //      //  object instead.
  //      final ResponseEntity<Resource> response =
  //        restTemplate.exchange(settlementEngineSettlementUrl, HttpMethod.POST, entity, Resource.class);
  //
  //      if (response.getStatusCode().is2xxSuccessful()) {
  //        logger
  //          .trace("Settlement request successfully sent.  settlementRequest={} settlementEngineUrl={}",
  //            initiateSettlementRequest,
  //            settlementEngineSettlementUrl
  //          );
  //        return response;
  //      } else {
  //        logger
  //          .error("Error sending settlement to Settlement engine. settlementRequest={} error={} ",
  //            initiateSettlementRequest,
  //            response.getStatusCode()
  //          );
  //        return response;
  //      }
  //    } catch (Exception e) {
  //      throw new SettlementEngineClientException(e.getMessage(), e, accountId);
  //    }
  //  }
  //
  //  /**
  //   * Given a message from a Peer's settlement engine, forward it to the local settlement engine by making an HTTP Post
  //   * request to `{settlementEngineBaseUrl}/{accounId}/message`, and then return the response.
  //   *
  //   * @param accountId
  //   * @param data      A byte array of data that two Settlement Engines can understand, but for which this client views
  //   *                  as opaque data.
  //   *
  //   * @return An opaque byte array response destined for the peer's settlment engine.
  //   */
  //  @Override
  //  public byte[] sendMessageFromPeer(final AccountId accountId, final byte[] data) {
  //    Objects.requireNonNull(accountId);
  //    Objects.requireNonNull(data);
  //
  //    try {
  //      HttpHeaders headers = new HttpHeaders();
  //      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
  //      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM));
  //
  //      final HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);
  //      final URI seMessageUrl = settlementEngineBaseUrl.newBuilder()
  //        .addPathSegment(ACCOUNTS)
  //        .addEncodedPathSegment(accountId.value())
  //        .addPathSegment(MESSAGES)
  //        .build().uri();
  //      final ResponseEntity<Resource> response =
  //        restTemplate.exchange(seMessageUrl, HttpMethod.POST, entity, Resource.class);
  //
  //      return ByteStreams.toByteArray(response.getBody().getInputStream());
  //    } catch (Exception e) {
  //      throw new SettlementEngineClientException(e.getMessage(), e, accountId);
  //    }
  //  }
}
