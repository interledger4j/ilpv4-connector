package org.interledger.connector.links.filters;

import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.interledger.connector.core.settlement.SettlementQuantity;
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

  public OutgoingBalanceLinkFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier,
    final BalanceTracker balanceTracker,
    final SettlementService settlementService
  ) {
    super(operatorAddressSupplier);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
    this.settlementService = Objects.requireNonNull(settlementService);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings destinationAccountSettings,
    final InterledgerPreparePacket outgoingPreparePacket,
    final LinkFilterChain filterChain
  ) {
    Objects.requireNonNull(destinationAccountSettings);
    Objects.requireNonNull(outgoingPreparePacket);
    Objects.requireNonNull(filterChain);

    final InterledgerResponsePacket responsePacket =
      filterChain.doFilter(destinationAccountSettings, outgoingPreparePacket);
    return new InterledgerResponsePacketMapper<InterledgerResponsePacket>() {

      @Override
      protected InterledgerResponsePacket mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {

        if (outgoingPreparePacket.getAmount().equals(BigInteger.ZERO)) {
          // No need to settle 0-value packets...
          return interledgerFulfillPacket;
        }

        /////////////////////////////
        // Update Balance for Fulfill
        /////////////////////////////

        final BalanceTracker.UpdateBalanceForFulfillResponse balanceForFulfillResponse;
        try {
          balanceForFulfillResponse = balanceTracker.updateBalanceForFulfill(
            destinationAccountSettings, outgoingPreparePacket.getAmount().longValue()
          );

          // TODO: Stats

        } catch (Exception e) {
          logger.error(String.format(
            "RECONCILIATION REQUIRED: Unable to update balance in Redis after receiving a valid Fulfillment. " +
              "outgoingPreparePacket=%s fulfillmentPacket=%s. error==%s",
            outgoingPreparePacket, interledgerFulfillPacket, e.getMessage()
            ), e // position the exception properly for logging.
          );

          // We received a fulfillment, but couldn't update the balance tracker for some reason. However, we want
          // to fulfill _AT ANY COST_, so we still return the fulfillment even though something went wrong here.
          return interledgerFulfillPacket;
        }

        /////////////////////////////
        // Maybe Settle...
        /////////////////////////////
        // If this operation fails for any reason, we still want to fulfill _AT ANY COST_, so log and eat any exceptions
        // but never get in the way of fulfilling a packet.
        try {
          // SettlementService throws an exception if no SE is configured, so only trigger it if there's an SE
          // configured.
          destinationAccountSettings.settlementEngineDetails().ifPresent(
            settlementEngineDetails -> {

              // TODO: Hibernate is using default composites currently, so it will errant populate a
              // settlementEngineDetails when this value should be `empty`. Once
              // https://github.com/sappenin/java-ilpv4-connector/issues/217 is fixed, this check can be removed
              // because the above `.ifPresent` line will work properly.
              if (settlementEngineDetails.baseUrl() == null) {
                logger.warn("TODO: Fix #217 to eliminate this WARN");
                return;
              }

              // Only trigger settlement if there's a Threshold...
              destinationAccountSettings.getBalanceSettings().getSettleThreshold()
                // ... and if the calculated clearingAmountToSettle is > the threshold (and not 0)
                .filter(settleThreshold -> balanceForFulfillResponse.clearingAmountToSettle() > 0 &&
                  balanceForFulfillResponse.clearingAmountToSettle() >= settleThreshold)
                .ifPresent(settleThreshold -> {
                  final UUID idempotencyId = UUID.randomUUID();

                  final SettlementQuantity settlementQuantityInClearingUnits = SettlementQuantity.builder()
                    .amount(BigInteger.valueOf(balanceForFulfillResponse.clearingAmountToSettle()))
                    .scale(destinationAccountSettings.getAssetScale())
                    .build();

                  // NOTE: This method is tightly-coupled to the fulfill balance processing above. Since the
                  // SettlementService is already tightly coupled, this is tolerable, but it might be clearer to have the
                  // SettlementService not roll-back if there's a problem (small case to be made that this method should
                  // handle the rollback).
                  settlementService.initiateLocalSettlement(
                    idempotencyId.toString(), destinationAccountSettings, settlementQuantityInClearingUnits
                  );
                });
            });
        } catch (Exception e) {
          logger.error(String.format(
            "While trying to initiate settlement engine payment: PreparePacket=%s; FulfillPacket=%s; Error=%s",
            outgoingPreparePacket, interledgerFulfillPacket, e.getMessage()
            ), e // position the exception properly for logging.
          );
        }

        // Happy-path: Return the fulfillment _IN ANY SCENARIO_ back to the filter-chain.
        return interledgerFulfillPacket;
      }

      @Override
      protected InterledgerResponsePacket mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        logger.warn(
          "Outgoing packet not applied due to ILP Reject. outgoingDestinationAccount={} amount={} newBalance={} preparePacket={} rejectPacket={}",
          destinationAccountSettings,
          outgoingPreparePacket.getAmount(),
          balanceTracker.getBalance(destinationAccountSettings.getAccountId()),
          outgoingPreparePacket,
          interledgerRejectPacket
        );

        // TODO: Stats
        //  this.stats.outgoingDataPacketValue.increment(account, {result :'rejected' },+amount)

        return interledgerRejectPacket;
      }
    }.map(responsePacket);
  }
}
