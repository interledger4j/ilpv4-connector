package com.sappenin.interledger.ilpv4.connector.links.filters;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTransferResult;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import static com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker.TRACKING_ACCOUNT_SUFFIX;


/**
 * An implementation of {@link LinkFilter} for updating balances of the account associated to the Link being filtered.
 */
public class OutgoingBalanceLinkFilter implements LinkFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final BalanceTracker balanceTracker;

  public OutgoingBalanceLinkFilter(final BalanceTracker balanceTracker) {
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId outgoingDestinationAccountId,
    final InterledgerPreparePacket outgoingPreparePacket,
    final LinkFilterChain filterChain
  ) {

    if (outgoingPreparePacket.getAmount().equals(BigInteger.ZERO)) {
      return filterChain.doFilter(outgoingDestinationAccountId, outgoingPreparePacket);
    } else {

      // We do nothing preemptively here (i.e. unlike for incoming packets) and wait until the packet is fulfilled
      // This means we always take the most conservative view of our balance with the upstream peer.
      try {
        final InterledgerResponsePacket responsePacket =
          filterChain.doFilter(outgoingDestinationAccountId, outgoingPreparePacket);

        // Handle Fulfill or Reject.
        new InterledgerResponsePacketHandler() {
          @Override
          protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
            // Decrease balance on fulfill
            decreaseAccountBalance(
              outgoingDestinationAccountId, outgoingPreparePacket.getAmount(),
              "Incoming Prepare Packet refunded due to ilp Reject"
            );
            // TODO: Enable Settle
            //this.maybeSettle(account);
            // TODO: Stats
            // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
            // this.stats.outgoingDataPacketValue.increment(account, { result : 'fulfilled' }, + amount)
          }

          @Override
          protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
            logger.info("Outgoing packet not applied due to ILP Reject. accountId={} amount={} newBalance={}",
              outgoingDestinationAccountId,
              outgoingPreparePacket.getAmount(),
              balanceTracker.getBalance(outgoingDestinationAccountId)
            );

            // TODO: Stats
            //  this.stats.outgoingDataPacketValue.increment(account, {result :'rejected' },+amount)
          }
        }.handle(responsePacket);


        // Return the response...
        return responsePacket;
      } catch (RuntimeException e) {
        logger.error("Outgoing packet not applied due to error. account.id={} amount={} newBalance={}",
          outgoingDestinationAccountId,
          outgoingPreparePacket.getAmount(),
          balanceTracker.getBalance(outgoingDestinationAccountId)
        );
        // TODO: Stats
        //this.stats.outgoingDataPacketValue.increment(account, { result : 'failed' }, + amount)
        throw e;
      }
    }
  }

  @VisibleForTesting
  protected void decreaseAccountBalance(AccountId accountId, BigInteger amount, String errorMessagePrefix) {
    // Ignore 0-amount packets.
    if (amount.equals(BigInteger.ZERO)) {
      return;
    } else {
      // This account is the account the Connector uses to track interactions with the account represented by accountId.
      final AccountId connectorTrackingAccountId = AccountId.of(accountId.value() + TRACKING_ACCOUNT_SUFFIX);

      // Decrease balance of accountId
      // TODO: Move ConnectorTracking account into the balance tracker.
      final BalanceTransferResult balanceTransferResult = this.balanceTracker.transferUnits(
        UUID.randomUUID(), accountId, connectorTrackingAccountId, amount
      );
      logger.debug("{}; BalanceTransferResult: {}", errorMessagePrefix, balanceTransferResult);
      // TODO: Log stats....
      // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
      // this.stats.incomingDataPacketValue.increment(account, { result : 'failed' }, + amount)
    }
  }

}
