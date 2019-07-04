package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.settlement;

import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotenceService;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentResponseInfo;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.core.settlement.Quantity;
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

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HeaderConstants.IDEMPOTENCY_KEY;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNT_ID;
import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.PathConstants.SLASH_SETTLEMENTS;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Allows a Settlement Engine to make requests on this Connector. This Controller is structured in such a way that it
 * can be extracted from the Connector runtime in order to be operated in a micro-services environment (e.g., HTTP
 * requests are accepted by an AccountSystem endpoint, and then all interactions with a Connector are performed via some
 * other interface, such as rsocket, possibly using queued messages).
 */
@RestController
@RequestMapping(SLASH)
public class SettlementController {

  private final IdempotenceService idempotenceService;
  private final SettlementService settlementService;

  public SettlementController(final IdempotenceService idempotenceService, final SettlementService settlementService) {
    this.idempotenceService = Objects.requireNonNull(idempotenceService);
    this.settlementService = Objects.requireNonNull(settlementService);
  }

  /**
   * <p>Called by the Settlement Engine when it detects an incoming settlement in order to notify this Connector's
   * accounting system. .</p>
   *
   * <p>This method allows the Settlement Engine to inform this Connector that money has been received on the
   * underlying ledger so that ths Connector can properly update its balances.</p>
   *
   * <p>Note that the settlement engine MAY accrue incoming settlement acknowledgements without immediately informing
   * this Connector.</p>
   *
   * @param requestIdString The idempotence identifier defined in the SE RFC (typed as a {@link String}, but should
   *                        always be a Type4 UUID).
   *
   * @return A {@link Quantity} that allows the accounting system indicate the amount it acknowledged receipt of so the
   * settlement engine can track the amount leftover (e.g., if the accounting system uses a unit of account that is
   * less-precise than the Settlement Engine).
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID + SLASH_SETTLEMENTS,
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<Quantity> creditIncomingSettlement(
    @RequestHeader(IDEMPOTENCY_KEY) final String requestIdString,
    @PathVariable final AccountId accountId,
    @RequestBody final Quantity quantity
  ) {
    Objects.requireNonNull(quantity);

    final UUID requestId;
    try {
      requestId = UUID.fromString(requestIdString);
    } catch (Exception e) {
      throw Problem.builder().withTitle("Invalid Idempotency Key").withStatus(Status.BAD_REQUEST).withDetail(
        "The `" + IDEMPOTENCY_KEY + "` header must be a Type4 UUID").build();
    }

    // Check Idempotence to avoid processing this request twice.
    return idempotenceService.getIdempotenceRecord(requestId)
      .map(priorRequest -> new ResponseEntity(
        priorRequest.responseBody(), priorRequest.responseHeaders(), priorRequest.responseStatus())
      )
      .orElseGet(() -> {
        // Only one thread is guaranteed to succeed here. E.g., Redis is single-threaded, so only one of these calls will
        // succeed, even under heavy load.
        if (idempotenceService.reserveRequestId(requestId)) {
          final Quantity settledQuantity = settlementService.handleIncomingSettlement(
            requestId, accountId, quantity
          );

          final HttpStatus status = HttpStatus.OK;
          final HttpHeaders headers = new HttpHeaders();
          final Link selfRel =
            linkTo(SettlementController.class).slash(SLASH_ACCOUNTS).slash(accountId).slash(SLASH_SETTLEMENTS)
              .withSelfRel();
          headers.setLocation(URI.create(selfRel.getHref()));
          headers.put(IDEMPOTENCY_KEY, Lists.newArrayList(requestId.toString()));

          // Update Redis for future Idempotent calls.
          final IdempotentResponseInfo response =
            IdempotentResponseInfo.builder()
              .requestId(requestId)
              .responseStatus(status)
              .responseHeaders(headers)
              .responseBody(settledQuantity)
              .build();
          final boolean responseCached = idempotenceService.updateIdempotenceRecord(response);

          if (responseCached) {
            return new ResponseEntity<>(settledQuantity, headers, status);
          } else {
            throw new RuntimeException("Unable to update IdempotenceRecord");
          }
        } else {
          // For some reason the threading guarantee assumed above didn't hold, so return a 409. This should only
          // happen in the unlikely event that a SE client makes the same request twice at approx the same time,
          // which should be extremely rare.
          return new ResponseEntity<Quantity>(HttpStatus.CONFLICT);
        }
      });
  }
}
