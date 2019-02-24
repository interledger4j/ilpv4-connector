package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

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
 * An implementation of {@link PacketSwitchFilter} for handling balances between two accounts/links in an ILP connector.
 * Only processes fulfillment responses.
 */
public class BalanceIlpPacketFilter implements PacketSwitchFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final BalanceTracker balanceTracker;

  public BalanceIlpPacketFilter(final BalanceTracker balanceTracker) {
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    // Preemptively increase the account balance....
    this.increaseAccountBalance(sourceAccountId, sourcePreparePacket.getAmount());
    try {
      final InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountId, sourcePreparePacket);

      // Handle Fulfill or Reject.
      new InterledgerResponsePacketHandler() {
        @Override
        protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          // TODO: Enable Settle
          //this.maybeSettle(account);
          // TODO: Stats
          // this.stats.incomingDataPacketValue.increment(account, { result : 'fulfilled' }, + amount)
        }

        @Override
        protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          // Refund on reject
          decreaseAccountBalance(
            sourceAccountId, sourcePreparePacket.getAmount(),
            "Incoming Prepare Packet refunded due to ilp Reject"
          );
          // TODO: Stats
          // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
          // this.stats.incomingDataPacketValue.increment(account, {result :'rejected' },+amount)
        }
      }.handle(responsePacket);

      // Return the response...
      return responsePacket;
    } catch (RuntimeException e) {
      // Refund on any kind of error...
      this.decreaseAccountBalance(
        sourceAccountId, sourcePreparePacket.getAmount(),
        String.format("Incoming PreparePacket refunded due to error (%s)", e.getMessage())
      );
      throw e;
    }
  }

  @VisibleForTesting
  protected void increaseAccountBalance(AccountId accountId, BigInteger amount) {
    // Ignore 0-amount packets.
    if (amount.equals(BigInteger.ZERO)) {
      return;
    } else {
      // This account is the account the Connector uses to track interactions with the account represented by accountId.
      final AccountId connectorTrackingAccountId = AccountId.of(accountId.value() + TRACKING_ACCOUNT_SUFFIX);

      // TODO: Move ConnectorTracking account into the balance tracker.
      // Increase balance of accountId on prepare
      final BalanceTransferResult balanceTransferResult = this.balanceTracker.transferUnits(
        UUID.randomUUID(), connectorTrackingAccountId, accountId, amount
      );
      logger.debug(
        "Account balance increased due to incoming ilp prepare. BalanceTransferResult: {}", balanceTransferResult
      );
      // TODO: Log stats....
      //this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
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

      // TODO: Move ConnectorTracking account into the balance tracker.
      // Decrease balance of accountId
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
