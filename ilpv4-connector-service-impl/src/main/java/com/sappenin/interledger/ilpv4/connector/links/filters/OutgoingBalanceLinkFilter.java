package com.sappenin.interledger.ilpv4.connector.links.filters;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;


/**
 * An implementation of {@link LinkFilter} for updating balances of the account associated to the Link being filtered.
 */
public class OutgoingBalanceLinkFilter extends AbstractLinkFilter implements LinkFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final BalanceTracker balanceTracker;

  public OutgoingBalanceLinkFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier,
    final BalanceTracker balanceTracker
  ) {
    super(operatorAddressSupplier);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId outgoingDestinationAccountId,
    final InterledgerPreparePacket outgoingPreparePacket,
    final LinkFilterChain filterChain
  ) {

    Objects.requireNonNull(outgoingDestinationAccountId);
    Objects.requireNonNull(outgoingPreparePacket);
    Objects.requireNonNull(filterChain);

    try {
      final InterledgerResponsePacket responsePacket =
        filterChain.doFilter(outgoingDestinationAccountId, outgoingPreparePacket);

      // Handle Fulfill or Reject.
      return new InterledgerResponsePacketMapper<InterledgerResponsePacket>() {
        @Override
        protected InterledgerResponsePacket mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          // The implementation has done nothing preemptively for outbound links until it receives a Fulfill
          // response. Thus, this code increment the receiver's account upon encountering a fulfill packet response
          // from the counterparty.
          try {
            balanceTracker.updateBalanceForFulfill(outgoingDestinationAccountId, outgoingPreparePacket.getAmount());

            // TODO: Enable Settle
            //this.maybeSettle(account);
            // TODO: Stats
            // this.stats.incomingDataPacketValue.increment(account, { result : 'fulfilled' }, + amount)


            return interledgerFulfillPacket;
          } catch (BalanceTrackerException e) {
            logger.error("RECONCILIATION REQUIRED: Unable to update balance in Redis. Reconciliation required! " +
                "OutgoingPreparePacket: {} ReceivedFulfillPacket: {}. OriginalError: {}",
              outgoingPreparePacket, interledgerFulfillPacket, e
            );

            return reject(
              outgoingDestinationAccountId, outgoingPreparePacket,
              InterledgerErrorCode.T00_INTERNAL_ERROR,
              "Received Fulfill from upstream, but could not apply balance change"
            );
          }
        }

        @Override
        protected InterledgerResponsePacket mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          logger.warn(
            "Outgoing packet not applied due to ILP Reject. targetAccountId={} amount={} newBalance={} preparePacket={} rejectPacket={}",
            outgoingDestinationAccountId,
            outgoingPreparePacket.getAmount(),
            balanceTracker.getBalance(outgoingDestinationAccountId),
            outgoingPreparePacket,
            interledgerRejectPacket
          );

          // TODO: Stats
          //  this.stats.outgoingDataPacketValue.increment(account, {result :'rejected' },+amount)

          return interledgerRejectPacket;
        }
      }.map(responsePacket);
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
