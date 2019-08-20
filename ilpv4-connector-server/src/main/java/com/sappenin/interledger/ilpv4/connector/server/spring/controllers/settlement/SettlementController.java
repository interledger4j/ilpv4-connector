package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.SettlementEngineAccountId;
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

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.ACCOUNT_ID;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_MESSAGES;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SE_ACCOUNT_ID;
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.web.IdempotenceCacheConfig.SETTLEMENT_IDEMPOTENCE;
import static org.interledger.ilpv4.connector.settlement.SettlementConstants.IDEMPOTENCY_KEY;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
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
   * <p>Caching Note: By setting {@link Cacheable#sync()} to true, this ensures that only one request will be able
   * to populate the cache, thus maintaing our idempotence requirements.</p>
   *
   * @param idempotencyKeyString      The idempotence identifier defined in the SE RFC (typed as a {@link String}, but
   *                                  should always be a Type4 UUID).
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} as supplied by the settlement engine. Note
   *                                  that settlement engines could theoretically store any type of identifier(either
   *                                  supplied by the Connector, or created by the engine). Thus, this implementation
   *                                  simply uses the settlement engines view of the world (i.e., a {@link
   *                                  SettlementEngineAccountId}) for communication.
   * @param settlementQuantity        A {@link SettlementQuantity}, as supplied by the Settlement Engine, that contains
   *                                  information about underlying money received for this account inside of the ledger
   *                                  that the settlement engine is tracking.
   *
   * @return A {@link SettlementQuantity} (in clearing units) that allows the clearing/accounting system indicate the
   * amount it acknowledged receipt of so the settlement engine can track the amount leftover (e.g., if the accounting
   * system uses a unit of account that is less-precise than the Settlement Engine).
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_SE_ACCOUNT_ID + SLASH_SETTLEMENTS,
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  @Cacheable(cacheNames = SETTLEMENT_IDEMPOTENCE, sync = true)
  public ResponseEntity<SettlementQuantity> creditIncomingSettlement(
    @RequestHeader(IDEMPOTENCY_KEY) final String idempotencyKeyString,
    @PathVariable(ACCOUNT_ID) final SettlementEngineAccountId settlementEngineAccountId,
    @RequestBody final SettlementQuantity settlementQuantity
  ) {
    this.requireIdempotenceId(idempotencyKeyString);
    Objects.requireNonNull(settlementEngineAccountId);
    Objects.requireNonNull(settlementQuantity);

    final SettlementQuantity settledSettlementQuantity = settlementService.onLocalSettlementPayment(
      SETTLEMENT_IDEMPOTENCE + ":" + idempotencyKeyString,
      settlementEngineAccountId,
      settlementQuantity
    );

    final HttpHeaders headers = new HttpHeaders();
    final Link selfRel = linkTo(SettlementController.class)
      .slash(SLASH_ACCOUNTS)
      .slash(settlementEngineAccountId)
      .slash(SLASH_SETTLEMENTS)
      .withSelfRel();

    headers.setLocation(URI.create(selfRel.getHref()));
    headers.put(IDEMPOTENCY_KEY, Lists.newArrayList(idempotencyKeyString));
    headers.setContentType(APPLICATION_JSON_UTF8);

    return new ResponseEntity<>(settledSettlementQuantity, headers, HttpStatus.OK);
  }

  /**
   * <p>Called by the Settlement Engine as a means of proxying messages from it (i.e., this Connector's Settlement
   * Engine) to this account's peer's Settlement Engine instance. All messages are tranmitted using ILPv4 links.</p>
   *
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} as supplied by the settlement engine. Note
   *                                  that settlement engines could theoretically store any type of identifier(either
   *                                  supplied by the Connector, or created by the engine). Thus, this implementation
   *                                  simply uses the settlement engines view of the world (i.e., a {@link
   *                                  SettlementEngineAccountId}) for communication.
   * @param settlementEngineMessage   A byte array of opaque data supplied by the settlement engine that should be sent
   *                                  to our peer's settlement engine for processing.
   *
   * @return A {@link SettlementQuantity} that allows the accounting system indicate the amount it acknowledged receipt
   * of so the settlement engine can track the amount leftover (e.g., if the accounting system uses a unit of account
   * that is less-precise than the Settlement Engine).
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_SE_ACCOUNT_ID + SLASH_MESSAGES,
    method = RequestMethod.POST,
    consumes = {APPLICATION_OCTET_STREAM_VALUE},
    produces = {APPLICATION_OCTET_STREAM_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<byte[]> processOutgoingMessageFromLocalSettlementEngine(
    @PathVariable(ACCOUNT_ID) final SettlementEngineAccountId settlementEngineAccountId,
    @RequestBody final byte[] settlementEngineMessage
  ) {
    Objects.requireNonNull(settlementEngineAccountId);
    Objects.requireNonNull(settlementEngineMessage);

    final byte[] responseBytes = settlementService.onLocalSettlementMessage(
      settlementEngineAccountId, settlementEngineMessage
    );

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_OCTET_STREAM);
    return new ResponseEntity<>(responseBytes, headers, HttpStatus.OK);
  }

  /**
   * Helper method to transform a String-based idempotency key into a {@link UUID}.
   *
   * @param idempotencyKey
   *
   * @return
   */
  private void requireIdempotenceId(final String idempotencyKey) {
    Objects.requireNonNull(idempotencyKey);
    if (idempotencyKey == null || idempotencyKey.length() <= 0) {
      throw Problem.builder()
        .withTitle("Idempotency header required")
        .withStatus(Status.BAD_REQUEST)
        .withDetail("The `" + IDEMPOTENCY_KEY + "` header must be supplied in order to accept a settlement")
        .build();
    }
  }
}
