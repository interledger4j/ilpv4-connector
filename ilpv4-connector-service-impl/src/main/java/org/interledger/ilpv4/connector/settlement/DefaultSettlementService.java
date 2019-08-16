package org.interledger.ilpv4.connector.settlement;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.events.LocalSettlementProcessedEvent;
import com.sappenin.interledger.ilpv4.connector.events.SettlementInitiatedEvent;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountBySettlementEngineAccountNotFoundProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineNotConfiguredProblem;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.core.settlement.ImmutableSettlementQuantity;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.ilpv4.connector.settlement.client.InitiateSettlementRequest;
import org.interledger.ilpv4.connector.settlement.client.InitiateSettlementResponse;
import org.interledger.ilpv4.connector.settlement.client.SendMessageRequest;
import org.interledger.ilpv4.connector.settlement.client.SendMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;

import static org.interledger.ilpv4.connector.core.Ilpv4Constants.ALL_ZEROS_CONDITION;
import static org.interledger.ilpv4.connector.settlement.SettlementConstants.PEER_DOT_SETTLE;

/**
 * The default implementation of {@link SettlementService}.
 */
public class DefaultSettlementService implements SettlementService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final BalanceTracker balanceTracker;
  private final LinkManager linkManager;
  private final AccountSettingsRepository accountSettingsRepository;
  private final SettlementEngineClient settlementEngineClient;
  private final EventBus eventBus;

  public DefaultSettlementService(
    final BalanceTracker balanceTracker,
    final LinkManager linkManager,
    final AccountSettingsRepository accountSettingsRepository,
    final SettlementEngineClient settlementEngineClient,
    final EventBus eventBus
  ) {
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.settlementEngineClient = Objects.requireNonNull(settlementEngineClient);
    this.eventBus = Objects.requireNonNull(eventBus);
  }

  @Override
  public SettlementQuantity onLocalSettlementPayment(
    final String idempotencyKey, final SettlementEngineAccountId settlementEngineAccountId,
    final SettlementQuantity incomingSettlementInSettlementUnits
  ) {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(settlementEngineAccountId, "settlementEngineAccountId must not be null");
    Objects.requireNonNull(incomingSettlementInSettlementUnits, "incomingSettlement must not be null");

    final AccountSettings accountSettings = this.accountSettingsRepository
      .findBySettlementEngineAccountId(settlementEngineAccountId)
      .orElseThrow(() -> new AccountBySettlementEngineAccountNotFoundProblem(settlementEngineAccountId));

    // Determine the normalized SettlementQuantity (i.e., translate from Settlement Ledger units to ILP Clearing Ledger
    // units
    final BigInteger settlementQuantityToAdjustInClearingLayer = NumberScalingUtils.translate(
      BigInteger.valueOf(incomingSettlementInSettlementUnits.amount()),
      incomingSettlementInSettlementUnits.scale(),
      accountSettings.getAssetScale()
    );

    // Update the balance in the clearing layer based upon what was settled to this account.
    this.balanceTracker.updateBalanceForIncomingSettlement(
      idempotencyKey, accountSettings.getAccountId(), settlementQuantityToAdjustInClearingLayer.longValue()
    );

    final ImmutableSettlementQuantity settledQuantity =
      SettlementQuantity.builder()
        .amount(settlementQuantityToAdjustInClearingLayer.longValue())
        .scale(accountSettings.getAssetScale())
        .build();

    // Notify the system that a settlement was processed...
    eventBus.post(LocalSettlementProcessedEvent.builder()
      .idempotencyKey(idempotencyKey)
      .settlementEngineAccountId(settlementEngineAccountId)
      .incomingSettlementInSettlementUnits(incomingSettlementInSettlementUnits)
      .processedQuantity(settledQuantity)
      .build());

    // This is the amount that was successfully adjusted in the clearing layer.
    return settledQuantity;
  }

  @Override
  public byte[] onLocalSettlementMessage(
    final SettlementEngineAccountId settlementEngineAccountId, final byte[] message
  ) {
    Objects.requireNonNull(settlementEngineAccountId, "settlementEngineAccountId must not be null");
    Objects.requireNonNull(message, "message must not be null");

    // Send the message to the Peer's Settlement Engine (as an ILP packet) directly on a Link. In this fashion, we
    // skip the router, balance, and FX logic because a Settlement Engine is not technically a Peer in the
    // traditional sense of the word.

    // Note that an alternative setup would be for the SE to connect to the Connector using something like
    // Ilp-over-Http using a `local` address potentially. This is currently not done because it adds complexity for
    // no real benefit (SE messages don't need true routing or FX or any other functionality that the Connector
    // packet-switch provides).

    final AccountSettings accountSettings = this.accountSettingsRepository
      .findBySettlementEngineAccountId(settlementEngineAccountId)
      .orElseThrow(() -> new AccountBySettlementEngineAccountNotFoundProblem(settlementEngineAccountId));

    final Link<? extends LinkSettings> link = linkManager.getOrCreateLink(accountSettings.getAccountId());

    InterledgerResponsePacket responsePacket = link.sendPacket(InterledgerPreparePacket.builder()
      .destination(PEER_DOT_SETTLE)
      .amount(BigInteger.ZERO)
      .expiresAt(Instant.now().plusSeconds(30))
      .data(message)
      // We don't care about the condition/fulfillment in peer-wise packet sending, so we just use this default.
      .executionCondition(ALL_ZEROS_CONDITION)
      .build());

    logger.trace(
      "Received ILP response packet from our peer's settlement engine " +
        "(`data` for the settlement engine is proxied in this response) accountId={} " +
        "settlementEngineAccountId={} responsePacket={}",
      accountSettings.getAccountId(), settlementEngineAccountId, responsePacket
    );

    return responsePacket.getData();
  }

  @Override
  public byte[] onSettlementMessageFromPeer(
    final AccountSettings accountSettings, final byte[] messageFromPeerSettlementEngine
  ) {
    Objects.requireNonNull(accountSettings, "accountSettings must not be null");
    Objects.requireNonNull(messageFromPeerSettlementEngine, "messageFromPeerSettlementEngine must not be null");

    // Only handle the request to send a message if the account has a configured Settlement Engine...
    return accountSettings.settlementEngineDetails()
      .map(settlementEngineDetails -> {
        // The `Prepare` packet's data was sent by the peer's settlement engine so we assume it is in a format that
        // our settlement engine will understand. Thus, simply send bytes directly to the Settlement Engine...
        final SendMessageResponse sendMessageResponse = settlementEngineClient.sendMessageFromPeer(
          accountSettings.getAccountId(),
          settlementEngineDetails.settlementEngineAccountId()
            .orElseGet(() -> SettlementEngineAccountId.of(accountSettings.getAccountId().value())),
          settlementEngineDetails.baseUrl(),
          SendMessageRequest.builder().data(messageFromPeerSettlementEngine).build()
        );

        return sendMessageResponse.data();
      })
      // Otherwise throw a settlement engine not-configured exception...
      .orElseThrow(() -> new SettlementEngineNotConfiguredProblem(accountSettings.getAccountId()));
  }

  @Override
  public SettlementQuantity initiateLocalSettlement(
    final String idempotencyKey,
    final AccountSettings accountSettings,
    final SettlementQuantity settlementQuantityInClearingUnits
  ) throws SettlementServiceException {

    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(accountSettings, "accountSettings must not be null");
    Objects.requireNonNull(settlementQuantityInClearingUnits, "settlementQuantityInClearingUnits must not be null");

    // 0 amount settlements should be ignored (negative values are not allowed in `SettlementQuantity`)
    if (settlementQuantityInClearingUnits.amount() <= 0) {
      logger.warn("SETTLEMENT initiated with 0 value");
      return SettlementQuantity.builder().amount(0).scale(settlementQuantityInClearingUnits.scale()).build();
    }

    // TODO: We don't want to block the ILP packet-flow thread here because the request to the SE might be a bit
    //  latent. Thus, we probably want to execute this "maybe settle" logic in a separate thread. However, we need to
    //  think a bit more about durability here. For example, the clearing balance has already been reduced, so if
    //  this call fails, we need to be sure to reset it. For now, we block the ILP flow, but in production this
    //  probably needs to be separated into a different thread so the ILP layer doesn't timeout accidentally.

    // TODO: https://github.com/sappenin/java-ilpv4-connector/issues/216
    // This request should be async, and likely scheduled for retry in a queue, just in-case the connector crashes
    // while this is being processed.
    return accountSettings.settlementEngineDetails()
      .map(settlementEngineDetails -> {
        try {

          /////////////////
          // CONTEXTUAL NOTE: Any system (Router or Settlement Engine) that makes a request should use a `Quantity` in
          // its own scaled units; and any response should be in the scale of the responder.

          final InitiateSettlementRequest initiateSettlementRequest = InitiateSettlementRequest.builder()
            .requestedSettlementAmount(BigInteger.valueOf(settlementQuantityInClearingUnits.amount()))
            .connectorAccountScale(settlementQuantityInClearingUnits.scale())
            .build();

          // This response will be in settlement engine units...
          // WARNING: the amount that the settlement engine commits to settle may diverge from the amount requested.
          final InitiateSettlementResponse initiateSettlementResponse = settlementEngineClient.initiateSettlement(
            accountSettings.getAccountId(),
            settlementEngineDetails.settlementEngineAccountId()
              .orElseGet(() -> SettlementEngineAccountId.of(accountSettings.getAccountId().value())),
            idempotencyKey,
            settlementEngineDetails.baseUrl(),
            initiateSettlementRequest
          );

          // Translate the Settled Amount into Clearing Units.
          final BigInteger settledAmountInClearingUnits = NumberScalingUtils.translate(
            initiateSettlementResponse.committedSettlementAmount(),
            initiateSettlementResponse.settlementEngineScale(),
            accountSettings.getAssetScale()
          );

          final AccountId accountId = accountSettings.getAccountId();
          final SettlementEngineAccountId settlementEngineAccountId =
            settlementEngineDetails.settlementEngineAccountId()
              .orElseThrow(() -> new SettlementEngineNotConfiguredProblem(accountId));
          final int clearingScale = accountSettings.getAssetScale();
          final int settlementScale = initiateSettlementResponse.settlementEngineScale();
          final BigInteger requestedClearingUnits = initiateSettlementRequest.requestedSettlementAmount();
          final BigInteger settledSettlementUnits = initiateSettlementResponse.committedSettlementAmount();
          final BigInteger settledClearingUnits = settledAmountInClearingUnits;

          logger.info(
            "SETTLEMENT RESULT: " +
              "AccountId={} SettlementEngineAccountId={} ClearingScale={} SettlementScale={} " +
              "RequestedClearingUnits={} SettledSettlementUnits={} SettledClearingUnits={}",
            accountId, settlementEngineAccountId, clearingScale, settlementScale,
            requestedClearingUnits, settledSettlementUnits, settledClearingUnits
          );

          final ImmutableSettlementQuantity settledQuantity = SettlementQuantity.builder()
            .amount(settledClearingUnits.longValue()) // TODO: Use BigInt here?
            .scale(clearingScale)
            .build();

          eventBus.post(SettlementInitiatedEvent.builder()
            .idempotencyKey(idempotencyKey)
            .accountSettings(accountSettings)
            .settlementQuantityInClearingUnits(settlementQuantityInClearingUnits)
            .processedQuantity(settledQuantity)
            .build());

          return settledQuantity;

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
              accountSettings.getAccountId(), settlementQuantityInClearingUnits.amount()
            );
          } catch (Exception e2) {
            // Swallowed (but logged) so that the other error message below is actually emitted too...
            logger.error(
              "Swallowed Exception while trying to roll-back balance transfer for failed settlement: "
                + e.getMessage(), e2
            );
          }

          final String errorMessage = String.format(
            "SETTLEMENT INITIATION FAILED settlementQuantityInClearingUnits=%s",
            settlementQuantityInClearingUnits
          );
          throw new SettlementServiceException(errorMessage, e, accountSettings.getAccountId());
        }
      })
      .orElseThrow(() -> new SettlementEngineNotConfiguredProblem(accountSettings.getAccountId()));
  }
}
