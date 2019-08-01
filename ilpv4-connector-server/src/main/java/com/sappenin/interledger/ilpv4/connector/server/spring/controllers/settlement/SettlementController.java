package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.spring.common.MediaTypes;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNT_ID;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_MESSAGES;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.IdempotenceCacheConfig.CACHE_NAME_MESSAGES;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.IdempotenceCacheConfig.CACHE_NAME_SETTLEMENTS;
import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

/**
 * Allows a Settlement Engine to make requests to this Connector. This Controller is structured in such a way that it
 * can be extracted from the Connector runtime in order to be operated in a micro-services environment (e.g., HTTP
 * requests are accepted by an Accounting System endpoint, and then all interactions with a Connector are performed via
 * some other interface, such as rsocket, possibly using queued messages).
 */
@RestController
@RequestMapping(SLASH)
public class SettlementController {

  private final SettlementService settlementService;

  public SettlementController(final SettlementService settlementService) {
    this.settlementService = Objects.requireNonNull(settlementService);
  }

  /**
   * <p>Called by the Settlement Engine when it detects an incoming settlement in order to notify this Connector's
   * accounting system.</p>
   *
   * <p>This method allows the Settlement Engine to inform this Connector that money has been received on the
   * underlying ledger so that ths Connector can properly update its balances.</p>
   *
   * <p>Note that the settlement engine MAY accrue incoming settlement acknowledgements without immediately informing
   * this Connector.</p>
   *
   * @param idempotencyKeyString The idempotence identifier defined in the SE RFC (typed as a {@link String}, but should
   *                             always be a Type4 UUID).
   * @param accountId            The {@link AccountId} as supplied by the SettlementEngine. Note that settlment engines
   *                             could theoretically store any type of identifier as supplied by the Connector during
   *                             settlement engine account creation. However, this implementation simply uses the
   *                             Connector's {@link AccountId} as this value.
   * @param settlementQuantity   A {@link SettlementQuantity}, as supplied by the Settlement Engine, that contains
   *                             information about underlying money received for this account inside of the ledger that
   *                             the settlement engine is tracking.
   *
   * @return A {@link SettlementQuantity} (in clearing units) that allows the clearing/accounting system indicate the
   * amount it acknowledged receipt of so the settlement engine can track the amount leftover (e.g., if the accounting
   * system uses a unit of account that is less-precise than the Settlement Engine).
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID + SLASH_SETTLEMENTS,
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  // Only one thread is guaranteed to succeed here if the underlying mechanism supports concurrent caching,
  // which Redis does.
  @Cacheable(cacheNames = CACHE_NAME_SETTLEMENTS, sync = true)
  public ResponseEntity<SettlementQuantity> creditIncomingSettlement(
    @RequestHeader(IDEMPOTENCY_KEY) final String idempotencyKeyString,
    @PathVariable final AccountId accountId,
    @RequestBody final SettlementQuantity settlementQuantity
  ) {
    final UUID idempotencyKey = toUuid(idempotencyKeyString);
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(settlementQuantity);

    final SettlementQuantity settledSettlementQuantity = settlementService.onLocalSettlementPayment(
      idempotencyKey, accountId, settlementQuantity
    );

    final HttpHeaders headers = new HttpHeaders();
    final Link selfRel =
      linkTo(SettlementController.class).slash(SLASH_ACCOUNTS).slash(accountId).slash(SLASH_SETTLEMENTS)
        .withSelfRel();
    headers.setLocation(URI.create(selfRel.getHref()));
    headers.put(IDEMPOTENCY_KEY, Lists.newArrayList(idempotencyKeyString));

    return new ResponseEntity<>(settledSettlementQuantity, headers, HttpStatus.OK);


    // Manual Code if @Cache doesn't work as intended...
    // See https://jira.spring.io/browse/DATAREDIS-678

    //    final UUID requestId = toUuid(idempotencyKey);
    //
    //    // Check Idempotence to avoid processing this request twice.
    //    return idempotentRequestCache.getHttpResponseInfo(requestId)
    //      .map(priorRequest -> new ResponseEntity(
    //        priorRequest.responseBody(), priorRequest.responseHeaders(), priorRequest.responseStatus())
    //      )
    //      .orElseGet(() -> {
    //        // Only one thread is guaranteed to succeed here. E.g., Redis is single-threaded, so only one of these calls will
    //        // succeed, even under heavy load.
    //        if (idempotentRequestCache.reserveRequestId(requestId)) {
    //          final SettlementQuantity settledSettlementQuantity = settlementService.onLocalSettlementPayment(
    //            requestId, accountId, settlementQuantity
    //          );
    //
    //          final HttpStatus status = HttpStatus.OK;
    //          final HttpHeaders headers = new HttpHeaders();
    //          final Link selfRel =
    //            linkTo(SettlementController.class).slash(SLASH_ACCOUNTS).slash(accountId).slash(SLASH_SETTLEMENTS)
    //              .withSelfRel();
    //          headers.setLocation(URI.create(selfRel.getHref()));
    //          headers.put(IDEMPOTENCY_KEY, Lists.newArrayList(requestId.toString()));
    //
    //          // Update Redis for future Idempotent calls.
    //          final HttpResponseInfo response =
    //            HttpResponseInfo.builder()
    //              .requestId(requestId)
    //              .responseStatus(status)
    //              .responseHeaders(headers)
    //              .responseBody(settledSettlementQuantity)
    //              .build();
    //          final boolean responseCached = idempotentRequestCache.updateHttpResponseInfo(response);
    //
    //          if (responseCached) {
    //            return new ResponseEntity<>(settledSettlementQuantity, headers, status);
    //          } else {
    //            throw new RuntimeException("Unable to update IdempotenceRecord"); // NOPMD - shouldn't ever happen.
    //          }
    //        } else {
    //          // For some reason the threading guarantee assumed above didn't hold, so return a 409. This should only
    //          // happen in the unlikely event that a SE client makes the same request twice at approx the same time,
    //          // which should be extremely rare.
    //          return new ResponseEntity<SettlementQuantity>(HttpStatus.CONFLICT);
    //        }
    //      });
  }

  /**
   * <p>Called by the Settlement Engine as a means of proxying messages from it (i.e., this Connector's Settlement
   * Engine) to this account's peer's Settlement Engine instance. All messages are tranmitted using ILPv4 links.</p>
   *
   * @param idempotencyKey The idempotence identifier defined in the SE RFC (typed as a {@link String}, but should
   *                       always be a Type4 UUID).
   *
   * @return A {@link SettlementQuantity} that allows the accounting system indicate the amount it acknowledged receipt
   * of so the settlement engine can track the amount leftover (e.g., if the accounting system uses a unit of account
   * that is less-precise than the Settlement Engine).
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID + SLASH_MESSAGES,
    method = RequestMethod.POST,
    consumes = {APPLICATION_OCTET_STREAM_VALUE},
    produces = {APPLICATION_OCTET_STREAM_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  // Only one thread is guaranteed to succeed here if the underlying mechanism supports concurrent caching,
  // which Redis does.
  @Cacheable(cacheNames = CACHE_NAME_MESSAGES, sync = true)
  public ResponseEntity<InterledgerResponsePacket> processOutgoingMessageFromLocalSettlementEngine(
    @RequestHeader(IDEMPOTENCY_KEY) final String idempotencyKey,
    @PathVariable final AccountId accountId,
    @RequestBody final byte[] settlementEngineMessage
  ) {
    Objects.requireNonNull(idempotencyKey);
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(settlementEngineMessage);

    final UUID idempotencyUuid = this.toUuid(idempotencyKey);
    final InterledgerResponsePacket responsePacket = settlementService.onLocalSettlementMessage(
      idempotencyUuid, accountId, settlementEngineMessage
    );

    final HttpHeaders headers = new HttpHeaders();
    headers.put(IDEMPOTENCY_KEY, Lists.newArrayList(idempotencyKey));
    return new ResponseEntity<>(responsePacket, headers, HttpStatus.OK);
  }

  // EXAMPLE Manual Redis Idempotence Check.

  //  return idempotentRequestCache.getHttpResponseInfo(idempotencyUuid)
  //    .map(priorRequest -> new ResponseEntity(
  //    priorRequest.responseBody(), priorRequest.responseHeaders(), priorRequest.responseStatus())
  //    )
  //    .orElseGet(() -> {
  //    // Only one thread is guaranteed to succeed here. E.g., Redis is single-threaded, so only one of these calls will
  //    // succeed, even under heavy load.
  //    if (idempotentRequestCache.reserveRequestId(idempotencyUuid)) {
  //      final InterledgerResponsePacket responsePacket = settlementService.onLocalSettlementMessage(
  //        idempotencyUuid, accountId, settlementEngineMessage
  //      );
  //
  //
  //      new InterledgerResponsePacketHandler() {
  //
  //        @Override
  //        protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
  //
  //        }
  //
  //        @Override
  //        protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
  //
  //        }
  //      }.handle(responsePacket);
  //
  //
  //      final HttpStatus status = HttpStatus.OK;
  //      final HttpHeaders headers = new HttpHeaders();
  //      final Link selfRel =
  //        linkTo(SettlementController.class).slash(SLASH_ACCOUNTS).slash(accountId).slash(SLASH_SETTLEMENTS)
  //          .withSelfRel();
  //      headers.setLocation(URI.create(selfRel.getHref()));
  //      headers.put(IDEMPOTENCY_KEY, Lists.newArrayList(idempotencyKey));
  //
  //
  //      ilpCodecContext.write(responsePacket, outputMessage.getBody());
  //
  //      // Update Redis for future Idempotent calls.
  //      final HttpResponseInfo response =
  //        HttpResponseInfo.builder()
  //          .requestId(idempotencyUuid)
  //          .responseStatus(status)
  //          .responseHeaders(headers)
  //          .responseBody()
  //          .build();
  //      final boolean responseCached = idempotentRequestCache.updateHttpResponseInfo(response);
  //
  //      if (responseCached) {
  //        return new ResponseEntity<>(responsePacket, headers, status);
  //      } else {
  //        throw new RuntimeException("Unable to update IdempotenceRecord"); // NOPMD - shouldn't ever happen.
  //      }
  //    } else {
  //      // For some reason the threading guarantee assumed above didn't hold, so return a 409. This should only
  //      // happen in the unlikely event that a SE client makes the same request twice at approx the same time,
  //      // which should be extremely rare.
  //      return new ResponseEntity<SettlementQuantity>(HttpStatus.CONFLICT);
  //    }
  //  });
  //

  /**
   * Helper method to transform a String-based idempotency key into a {@link UUID}.
   *
   * @param idempotencyKey
   *
   * @return
   */
  private UUID toUuid(final String idempotencyKey) {
    Objects.requireNonNull(idempotencyKey);
    try {
      return UUID.fromString(idempotencyKey);
    } catch (Exception e) {
      throw Problem.builder().withTitle("Invalid Idempotency Key").withStatus(Status.BAD_REQUEST).withDetail(
        "The `" + IDEMPOTENCY_KEY + "` header must be a Type4 UUID").build();
    }
  }
}
