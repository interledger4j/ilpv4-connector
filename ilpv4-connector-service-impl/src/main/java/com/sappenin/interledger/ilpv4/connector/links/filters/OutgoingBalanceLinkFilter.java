package com.sappenin.interledger.ilpv4.connector.links.filters;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceChangeResult;
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
import java.util.UUID;
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

    // We do nothing preemptively here (i.e. unlike for incoming packets) and wait until the packet is fulfilled
    // This means we always take the most conservative view of our balance with the upstream peer.

    final UUID balanceAdjustmentTxId = UUID.randomUUID();
    try {
      final InterledgerResponsePacket responsePacket =
        filterChain.doFilter(outgoingDestinationAccountId, outgoingPreparePacket);

      // Handle Fulfill or Reject.
      return new InterledgerResponsePacketMapper<InterledgerResponsePacket>() {
        @Override
        protected InterledgerResponsePacket mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {

          try {
            // Decrease balance on fulfill
            final BalanceChangeResult result = balanceTracker.adjustBalance(
              balanceAdjustmentTxId, outgoingDestinationAccountId, outgoingPreparePacket.getAmount().negate()
            );
            // NOT_ATTEMPTED can occur if the BalanceChange is 0, so we only reject here if there was a
            // problem with the balance tracker.
            if (result.balanceTransferStatus() == BalanceChangeResult.Status.FAILED) {
              // TODO: Reconsider this. If this happens, it means the peer fulfilled, but we're unable to fulfill with
              //  our connector because the Balance tracker was unable to be updated. This should trigger a
              //  reconciliation event of some sort.
              logger.error(
                "Outgoing account Fulfilled, but the BalanceTracker update failed. RECONCILATION WARNING: {}", result
              );
              return reject(
                outgoingDestinationAccountId, outgoingPreparePacket,
                InterledgerErrorCode.F99_APPLICATION_ERROR, "Unable to apply BalanceChange: " + result
              );
            } else {
              // TODO: Enable Settle
              //this.maybeSettle(account);
              // TODO: Stats
              // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
              // this.stats.outgoingDataPacketValue.increment(account, { result : 'fulfilled' }, + amount)
              return interledgerFulfillPacket;
            }
          } catch (BalanceTrackerException e) {
            logger.error(e.getMessage(), e);
            return reject(
              outgoingDestinationAccountId, outgoingPreparePacket,
              InterledgerErrorCode.F99_APPLICATION_ERROR, "Unable to apply BalanceChange: " + e.getMessage()
            );
          }
        }

        @Override
        protected InterledgerResponsePacket mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          logger.info("Outgoing packet not applied due to ILP Reject. accountId={} amount={} newBalance={}",
            outgoingDestinationAccountId,
            outgoingPreparePacket.getAmount(),
            balanceTracker.getBalance(outgoingDestinationAccountId)
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

  //  @VisibleForTesting
  //  protected void decreaseAccountBalance(AccountId accountId, BigInteger amount, String errorMessagePrefix) {
  //    // Ignore 0-amount packets.
  //    if (amount.equals(BigInteger.ZERO)) {
  //      return;
  //    } else {
  //      // Decrease balance of accountId
  //      final BalanceChangeResult balanceChangeResult = this.balanceTracker.adjustBalance(
  //        UUID.randomUUID(), accountId, amount.negate()
  //      );
  //      logger.debug("{}; BalanceChangeResult: {}", errorMessagePrefix, balanceChangeResult);
  //      // TODO: Log stats....
  //      // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
  //      // this.stats.incomingDataPacketValue.increment(account, { result : 'failed' }, + amount)
  //    }
  //  }

}
