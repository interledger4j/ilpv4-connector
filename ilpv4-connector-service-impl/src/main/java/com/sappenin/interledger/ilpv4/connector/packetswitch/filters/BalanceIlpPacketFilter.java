package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceChangeResult;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for handling balances between two accounts/links in an ILP connector.
 * Only processes fulfillment responses.
 */
public class BalanceIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final BalanceTracker balanceTracker;

  public BalanceIlpPacketFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier, final BalanceTracker balanceTracker
  ) {
    super(operatorAddressSupplier);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    final UUID balanceAdjustmentTxId = UUID.randomUUID();
    try {
      // Preemptively decrease the account balance....
      this.balanceTracker.adjustBalance(balanceAdjustmentTxId, sourceAccountId, sourcePreparePacket.getAmount());

      final InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountId, sourcePreparePacket);

      // Handle Fulfill or Reject.
      new InterledgerResponsePacketHandler() {
        @Override
        protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          // The Receiver's balance is always adjusted in the outgoing filter.

          // TODO: Enable Settle
          //this.maybeSettle(account);
          // TODO: Stats
          // this.stats.incomingDataPacketValue.increment(account, { result : 'fulfilled' }, + amount)
        }

        @Override
        protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          // Reverse the receiver on reject. The sender will be reversed in the Packet filter.
          final BalanceChangeResult balanceChangeResult = balanceTracker.adjustBalance(
            balanceAdjustmentTxId, sourceAccountId, sourcePreparePacket.getAmount().negate()
          );

          // TODO: If we're unable to apply the reversal, then the receiver has money that it shouldn't have, so this
          //  should trigger some sort of reconcilation.

          // TODO: Stats
          // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
          // this.stats.incomingDataPacketValue.increment(account, {result :'rejected' },+amount)
        }
      }.handle(responsePacket);

      // Return the response...
      return responsePacket;
    } catch (BalanceTrackerException e) {
      // Refund on any kind of error...
      BalanceChangeResult balanceChangeResult = balanceTracker.adjustBalance(
        balanceAdjustmentTxId, sourceAccountId, sourcePreparePacket.getAmount().negate()
      );
      logger.error(
        "Incoming Prepare Packet refunded (due to Internal Error ) with BalanceChangeResult: {}, {}",
        balanceChangeResult, e
      );
      // TODO: Stats
      // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
      // this.stats.incomingDataPacketValue.increment(account, {result :'rejected' },+amount)

      // TODO: Is this correct? If the BalanceTracker is offline, then what will this manifest as to the sender?
      throw e;
    }
  }
}
