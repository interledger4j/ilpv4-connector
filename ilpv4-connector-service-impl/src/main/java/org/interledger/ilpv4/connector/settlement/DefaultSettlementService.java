package org.interledger.ilpv4.connector.settlement;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementServiceException;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineNotConfiguredProblem;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.sappenin.interledger.ilpv4.connector.settlement.SettlementConstants.PEER_DOT_SETTLE;

/**
 * The default implementation of {@link SettlementService}.
 */
public class DefaultSettlementService implements SettlementService {

  // We don't care about the condition/fulfillment in peer-wise packet sending, so we just use a fulfillment of all 0s
  // and the corresponding condition.
  private static final InterledgerFulfillment PEER_PROTOCOL_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  private static final InterledgerCondition PEER_PROTOCOL_CONDITION = PEER_PROTOCOL_FULFILLMENT.getCondition();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final BalanceTracker balanceTracker;
  private final LinkManager linkManager;
  private final AccountSettingsRepository accountSettingsRepository;
  private final RestTemplate settlementEngineRestTemplate;

  // Every Account will potentially settle using a different Settlement Engine (though each account will only settle
  // with a single engine). This Cache stores HTTP clients for each Settlement Engine on an as-needed basis.
  private final Cache<HttpUrl, SettlementEngineClient> settlementEngineClientCache;

  /**
   * Exists only for testing
   */
  @VisibleForTesting
  protected DefaultSettlementService(
    final BalanceTracker balanceTracker,
    final LinkManager linkManager,
    final AccountSettingsRepository accountSettingsRepository,
    final RestTemplate settlementEngineRestTemplate
  ) {
    this(
      balanceTracker,
      linkManager,
      accountSettingsRepository,
      settlementEngineRestTemplate,
      Caffeine.newBuilder()
        .expireAfterAccess(2, TimeUnit.MINUTES)
        .maximumSize(5000)
        .build()
    );
  }

  public DefaultSettlementService(
    final BalanceTracker balanceTracker,
    final LinkManager linkManager,
    final AccountSettingsRepository accountSettingsRepository,
    final RestTemplate settlementEngineRestTemplate,
    final Cache<HttpUrl, SettlementEngineClient> settlementEngineClientCache
  ) {
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.settlementEngineRestTemplate = Objects.requireNonNull(settlementEngineRestTemplate);
    this.settlementEngineClientCache = Objects.requireNonNull(settlementEngineClientCache);
  }

  @Override
  public SettlementQuantity onLocalSettlementPayment(
    final UUID idempotencyKey, final AccountId accountId, final SettlementQuantity incomingSettlementInSettlementUnits
  ) {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(incomingSettlementInSettlementUnits, "incomingSettlement must not be null");

    final AccountSettings accountSettings = this.accountSettingsRepository
      .findByAccountId(accountId).orElseThrow(() -> new AccountNotFoundProblem(accountId));

    // Determine the normalized SettlementQuantity (i.e., translate from Settlement Ledger units to ILP Clearing Ledger
    // units
    final SettlementQuantity settlementQuantityToAdjustInClearingLayer =
      NumberScalingUtils.translate(incomingSettlementInSettlementUnits, accountSettings.getAssetScale());

    // Update the balance in the clearing layer based upon what was settled to this account.
    this.balanceTracker.updateBalanceForIncomingSettlement(
      idempotencyKey, accountSettings.getAccountId(), settlementQuantityToAdjustInClearingLayer.amount()
    );

    // This is the amount that was successfully adjusted in the clearing layer.
    return settlementQuantityToAdjustInClearingLayer;
  }

  @Override
  public InterledgerResponsePacket onLocalSettlementMessage(
    final UUID idempotencyKey,
    final AccountId accountId,
    final byte[] message
  ) {

    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(message, "message must not be null");

    // Send the message to the Peer's Settlement Engine (as an ILP packet) directly on a Link. In this fashion, we
    // skip the router, balance, and FX logic because a Settlement Engine is not technically a Peer in the
    // traditional sense of the word.

    // Note that an alternative setup would be for the SE to connect to the Connector using something like
    // Ilp-over-Http using a `local` address potentially. This is currently not done because it adds complexity for
    // no real benefit (SE messages don't need true routing or FX or any other functionality that the Connector
    // packet-switch provides).

    final Link<? extends LinkSettings> link = linkManager.getOrCreateLink(accountId);

    InterledgerResponsePacket responsePacket = link.sendPacket(InterledgerPreparePacket.builder()
      .destination(PEER_DOT_SETTLE)
      .amount(BigInteger.ZERO)
      .expiresAt(Instant.now().plusSeconds(30))
      .data(message)
      // We don't care about the condition/fulfillment in peer-wise packet sending, so we just use this default.
      .executionCondition(PEER_PROTOCOL_CONDITION)
      .build());

    logger.trace(
      "Received responsePacket from remote Settlement Engine for Account(`{}`): {}", accountId, responsePacket
    );

    return responsePacket;
  }

  @Override
  public InterledgerResponsePacket onSettlementMessageFromPeer(
    final AccountSettings accountSettings, final InterledgerPreparePacket packetFromPeer
  ) {

    Objects.requireNonNull(accountSettings, "accountSettings must not be null");
    Objects.requireNonNull(packetFromPeer, "packetFromPeer must not be null");

    // Only handle the request to send a message if the account has a configured Settlement Engine...
    return accountSettings.settlementEngineDetails()
      .map(settlementEngineDetails -> {

        // Send the message (i.e., the payload's data bytes) directly to the Settlement Engine...
        // https://se.example.com/
        try {
          final SettlementEngineClient settlementEngineClient =
            getSettlementEngineClientHelper(settlementEngineDetails.baseUrl());

          // The `Prepare` packet's data was sent by the peer's settlement engine so we assume it is in a format that
          // our settlement engine will understand
          final byte[] seResponse = settlementEngineClient.sendMessageFromPeer(
            accountSettings.getAccountId(), packetFromPeer.getData()
          );

          // If no error, then fulfill
          return InterledgerFulfillPacket.builder()
            .fulfillment(PEER_PROTOCOL_FULFILLMENT)
            .data(seResponse)
            .build();
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          return InterledgerRejectPacket.builder()
            .triggeredBy(PEER_DOT_SETTLE)
            .message(String.format(
              "Error sending message to settlement engine for Account %s: %s",
              accountSettings.getAccountId(), e.getMessage()
            ))
            .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
            .build();
        }
      })
      // Otherwise just reject...
      .orElseGet(() -> {
        final String errorMessage = String.format(
          "Got settlement packet from account `%s` but there is no settlement engine configured for it",
          accountSettings.getAccountId()
        );
        logger.error(errorMessage);
        return InterledgerRejectPacket.builder()
          .triggeredBy(PEER_DOT_SETTLE)
          .message(errorMessage)
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .build();
      });
  }

  @Override
  public SettlementQuantity initiateSettlementForFulfillThreshold(
    UUID idempotencyKey, AccountSettings accountSettings, SettlementQuantity requestedSettlementQuantityInClearingUnits
  ) throws SettlementServiceException {

    // 0 amount settlements should be ignored (negative values are not allowed in `SettlementQuantity`)
    if (requestedSettlementQuantityInClearingUnits.amount() <= 0) {
      logger.warn("SETTLEMENT initiated with 0 value");
      return SettlementQuantity.builder().amount(0).scale(requestedSettlementQuantityInClearingUnits.scale()).build();
    }

    return accountSettings.settlementEngineDetails()
      .map(settlementEngineDetails -> {
        try {
          final SettlementEngineClient settlementEngineClient =
            getSettlementEngineClientHelper(settlementEngineDetails.baseUrl());

          // Convert the amount into a Quantity in the SE's scale...
          final SettlementQuantity requestedSettlementQuantityInSettlementUnits = NumberScalingUtils.translate(
            requestedSettlementQuantityInClearingUnits, settlementEngineDetails.assetScale()
          );

          ////////////////////
          // The actual HTTP request
          ////////////////////
          final ResponseEntity<Resource> response = settlementEngineClient.initiateSettlement(
            accountSettings.getAccountId(), idempotencyKey, requestedSettlementQuantityInSettlementUnits
          );

          // See https://github.com/sappenin/java-ilpv4-connector/issues/216
          // Note that if the Connector crashes here, then the balance will not have been updated, and will be
          // incorrect. We should devise a more robust recovery system here so that the settlement response
          // can be durably processed even if the process crashes here.
          if (response.getStatusCode().is2xxSuccessful()) {
            // TODO: Validate response type/exception style from the actual response vs an exception.

            // The amount settled may diverge from the amount requested (e.g., scale differences, etc).
            final long settledSettlementUnits = new BigInteger(response.getBody().toString()).longValue();

            // Not translated - this return value is in settlement units...
            final SettlementQuantity settledSettlementQuantityInSettlementUnits = SettlementQuantity.builder()
              .amount(settledSettlementUnits)
              .scale(settlementEngineDetails.assetScale())
              .build();

            // Convert to clearing units...
            final SettlementQuantity settledSettlementQuantityInClearingUnits = NumberScalingUtils.translate(
              settledSettlementQuantityInSettlementUnits, accountSettings.getAssetScale()
            );

            logger.info(
              "SETTLEMENT INITIATED (AccountId: `{}`): " +
                "CLEARING units REQUESTED: `{}`; " +
                "SETTLEMENT units REQUESTED: `{}`; " +
                "SETTLEMENT units SETTLED: `{}`; " +
                "CLEARING units SETTLED: `{}`",
              accountSettings.getAccountId(),

              requestedSettlementQuantityInClearingUnits,
              requestedSettlementQuantityInSettlementUnits,
              settledSettlementQuantityInSettlementUnits,
              settledSettlementQuantityInClearingUnits
            );

            return settledSettlementQuantityInClearingUnits;
          } else {
            throw new SettlementServiceException(
              String.format(
                "Unable to initiate settlement payment for: %s", requestedSettlementQuantityInSettlementUnits
              ),
              accountSettings.getAccountId()
            );
          }
        } catch (Exception e) { // If anything goes wrong, rollback the preemptive balance update.
          //////////////////
          // Refund the Settlement
          //////////////////
          // The `updateBalanceForFulfill.lua` script will preemptively reduce the clearing balance if a settlement
          // payment is determined to be necessary (i.e.,  balanceForFulfillResponse.settleAmount > 0). In this case,
          // there is a small window of time (the delta between this balance change in Redis in the Fulfill script) and
          // the moment that the settlement-engine accepts the request for settlement payment) where the actual
          // balance in Redis is less than it should be. This is tolerable because this amount of time will always be
          // small because of the design of the settlement engine API is asynchronous (meaning, when a request is made
          // to the settlement engine, the SE will accept the request and return (within milliseconds) with a guarantee
          // that the settlement payment will _eventually_ be completed. Because of this settlement_engine guarantee,
          // the Connector can operate as-if the settlement engine has completed. However, if the request to the
          // settlement-engine instead fails, the amount deducted in the Fulfill script needs to be re-added. This
          // occurs here.
          try {
            balanceTracker.updateBalanceForOutgoingSettlementRefund(
              accountSettings.getAccountId(), requestedSettlementQuantityInClearingUnits.amount()
            );
          } catch (Exception e2) {
            // Swallowed by logged so that the errorMessage below is actually emitted...
            logger.error("Swallowed Exception: " + e.getMessage(), e2);
          }

          final String errorMessage = String.format(
            "SETTLEMENT INITIATION FAILED for requested settlement: %s", requestedSettlementQuantityInClearingUnits
          );
          throw new SettlementServiceException(errorMessage, e, accountSettings.getAccountId());
        }
      })
      .orElseThrow(() -> new SettlementEngineNotConfiguredProblem(accountSettings.getAccountId()));
  }

  /**
   * Helper to access a {@link SettlementEngineClient} from the cache, or else create a new one.
   *
   * @param settlementEngineUrl The unique URL of the settlement engine to obtain. Note that a given settlement engine
   *                            can be accessed by potentially multiple accounts.
   *
   * @return A {@link SettlementEngineClient}.
   */
  private SettlementEngineClient getSettlementEngineClientHelper(final HttpUrl settlementEngineUrl) {
    Objects.requireNonNull(settlementEngineUrl);

    return settlementEngineClientCache.get(
      settlementEngineUrl,
      (settlementEngineBaseUrl) -> new DefaultSettlementEngineClient(
        settlementEngineRestTemplate, settlementEngineBaseUrl
      )
    );
  }
}
