package org.interledger.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.BalanceTracker.UpdateBalanceForFulfillResponse;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.events.OutgoingSettlementInitiationFailedEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationSucceededEvent;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.connector.settlement.SettlementServiceException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFilter} for updating balances of the account associated to the Link being filtered.
 */
public class OutgoingBalanceLinkFilter extends AbstractLinkFilter implements LinkFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final BalanceTracker balanceTracker;
  private final SettlementService settlementService;
  private final EventBus eventBus;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} of this Connector's operator {@link InterledgerAddress}.
   * @param balanceTracker          A {@link BalanceTracker}.
   * @param settlementService       A {@link SettlementService}.
   * @param eventBus                An {@link EventBus}.
   */
  public OutgoingBalanceLinkFilter(
      final Supplier<InterledgerAddress> operatorAddressSupplier,
      final BalanceTracker balanceTracker,
      final SettlementService settlementService,
      final EventBus eventBus
  ) {
    super(operatorAddressSupplier);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    this.settlementService = Objects.requireNonNull(settlementService);
    this.eventBus = Objects.requireNonNull(eventBus);
  }

  @Override
  public InterledgerResponsePacket doFilter(
      final AccountSettings destinationAccountSettings,
      final InterledgerPreparePacket outgoingPreparePacket,
      final LinkFilterChain filterChain
  ) {
    Objects.requireNonNull(destinationAccountSettings, "destinationAccountSettings must not be null");
    Objects.requireNonNull(outgoingPreparePacket, "outgoingPreparePacket must not be null");
    Objects.requireNonNull(filterChain, "filterChain must not be null");

    final InterledgerResponsePacket responsePacket
        = filterChain.doFilter(destinationAccountSettings, outgoingPreparePacket);

    responsePacket.handle(
        //////////////////////
        // If FulfillPacket...
        //////////////////////
        (interledgerFulfillPacket) -> {
          if (UnsignedLong.ZERO.equals(outgoingPreparePacket.getAmount())) {
            // No need to settle 0-value packets...
            return;
          }

          /////////////////////////////
          // Update Balance for Fulfill
          /////////////////////////////

          final BalanceTracker.UpdateBalanceForFulfillResponse balanceForFulfillResponse;
          try {
            balanceForFulfillResponse = balanceTracker.updateBalanceForFulfill(
                destinationAccountSettings, outgoingPreparePacket.getAmount().longValue()
            );
          } catch (Exception e) {
            logger.error(String.format(
                "RECONCILIATION REQUIRED: Unable to update balance in Redis after receiving a valid Fulfillment. " +
                    "outgoingPreparePacket=%s fulfillmentPacket=%s. error==%s",
                outgoingPreparePacket, interledgerFulfillPacket, e.getMessage()
                ), e // position the exception properly for logging.
            );

            // We received a fulfillment, but couldn't update the balance tracker for some reason. However, we want
            // to fulfill _AT ANY COST_, so return without throwing nor checking if settlement is required.
            return;
          }

          // Check if a settlement payment should be initiated.
          this.maybeSettle(
              destinationAccountSettings, outgoingPreparePacket, interledgerFulfillPacket, balanceForFulfillResponse
          );
        },
        //////////////////////
        // If Reject Packet...
        //////////////////////
        (interledgerRejectPacket) -> logger.warn(
            "Outgoing packet not applied due to ILP Reject. outgoingDestinationAccount={} amount={} newBalance={} "
                + "outgoingPreparePacket={} rejectPacket={}",
            destinationAccountSettings,
            outgoingPreparePacket.getAmount(),
            balanceTracker.balance(destinationAccountSettings.accountId()),
            outgoingPreparePacket,
            interledgerRejectPacket
        )
    );

    // ALWAYS return the Response packet _AT ANY COST_ so this connector doesn't lose money.
    return responsePacket;
  }

  /**
   * Determine whether or not a settlement payment should be initiated.
   *
   * WARNING: If this operation fails for any reason, we still want to fulfill _AT ANY COST_, so log and eat any
   * exceptions but never get in the way of fulfilling a packet.
   *
   * @param destinationAccountSettings An {@link AccountSettings} for the account being settled with.
   * @param outgoingPreparePacket      A {@link InterledgerPreparePacket} representing the outgoing packet sent to
   *                                   {@code destinationAccountSettings}.
   * @param interledgerFulfillPacket   The {@link InterledgerFulfillPacket} that corresponds to {@code
   *                                   outgoingPreparePacket}.
   * @param balanceForFulfillResponse  The {@link UpdateBalanceForFulfillResponse} computed by the balance tracker.
   */
  private void maybeSettle(
      final AccountSettings destinationAccountSettings,
      final InterledgerPreparePacket outgoingPreparePacket,
      final InterledgerFulfillPacket interledgerFulfillPacket,
      final BalanceTracker.UpdateBalanceForFulfillResponse balanceForFulfillResponse
  ) {

    try {
      // SettlementService throws an exception if no SE is configured, so only trigger it if there's an SE
      // configured.
      destinationAccountSettings.settlementEngineDetails().ifPresent(
          settlementEngineDetails -> {

            // Only trigger settlement if there's a Threshold...
            destinationAccountSettings.balanceSettings().settleThreshold()
                // ... and if the calculated clearingAmountToSettle is > the threshold (and not 0)
                .filter(settleThreshold -> balanceForFulfillResponse.clearingAmountToSettle() > 0 &&
                    balanceForFulfillResponse.clearingAmountToSettle() >= settleThreshold)
                .ifPresent(settleThreshold -> {
                  final UUID idempotencyId = UUID.randomUUID();

                  final SettlementQuantity settlementQuantityInClearingUnits = SettlementQuantity.builder()
                      .amount(BigInteger.valueOf(balanceForFulfillResponse.clearingAmountToSettle()))
                      .scale(destinationAccountSettings.assetScale())
                      .build();

                  try {
                    // NOTE: This method is tightly-coupled to the fulfill balance processing above. Since the
                    // SettlementService is already tightly coupled, this is tolerable, but it might be clearer to have the
                    // SettlementService not roll-back if there's a problem (small case to be made that this method should
                    // handle the rollback).
                    final SettlementQuantity processedQuantityInClearingUnits = settlementService
                        .initiateLocalSettlement(
                            idempotencyId.toString(), destinationAccountSettings, settlementQuantityInClearingUnits
                        );

                    eventBus.post(OutgoingSettlementInitiationSucceededEvent.builder()
                        .accountSettings(destinationAccountSettings)
                        .idempotencyKey(idempotencyId.toString())
                        .settlementQuantityInClearingUnits(settlementQuantityInClearingUnits)
                        .processedQuantityInClearingUnits(processedQuantityInClearingUnits)
                        .build());
                  } catch (Exception e) {
                    eventBus.post(OutgoingSettlementInitiationFailedEvent.builder()
                        .accountSettings(destinationAccountSettings)
                        .idempotencyKey(idempotencyId.toString())
                        .settlementQuantityInClearingUnits(settlementQuantityInClearingUnits)
                        .settlementServiceException(new SettlementServiceException(
                            e,
                            destinationAccountSettings.accountId(),
                            settlementEngineDetails.settlementEngineAccountId().get()
                        ))
                        .build());
                    throw e;
                  }
                });
          });
    } catch (Exception e) {
      logger.error(String.format(
          "While trying to initiate settlement engine payment: PreparePacket=%s; FulfillPacket=%s; Error=%s",
          outgoingPreparePacket, interledgerFulfillPacket, e.getMessage()
          ), e // position the exception properly for logging.
      );
    }
  }
}
