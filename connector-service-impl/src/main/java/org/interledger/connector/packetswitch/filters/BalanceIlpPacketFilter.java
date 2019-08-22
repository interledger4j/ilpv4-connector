package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.BalanceTrackerException;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.util.Objects;

/**
 * An implementation of {@link PacketSwitchFilter} for handling balance updates for a given ILP request/response flow.
 */
public class BalanceIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final BalanceTracker balanceTracker;

  public BalanceIlpPacketFilter(
    final PacketRejector packetRejector, final BalanceTracker balanceTracker
  ) {
    super(packetRejector);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    try {
      // Preemptively decrease the account balance....
      this.balanceTracker.updateBalanceForPrepare(
        sourceAccountSettings.getAccountId(), sourcePreparePacket.getAmount().longValue(),
        sourceAccountSettings.getBalanceSettings().getMinBalance()
      );

      // TODO: Stats (Should half of this instead be in the balance tracker?)
      // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
      // this.stats.incomingDataPacketValue.increment(account, {result :'rejected' },+amount)

    } catch (BalanceTrackerException e) {
      // If there's an error, it means the prepare balance update was not applied, so simply log the exception and
      // reject.
      logger.error(e.getMessage(), e);
      return packetRejector.reject(
        sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY, ""
      );
    }

    final InterledgerResponsePacket responsePacket = filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);

    // Handle Fulfill or Reject, but return `responsePacket` in either case.
    return new InterledgerResponsePacketMapper<InterledgerResponsePacket>() {

      @Override
      protected InterledgerResponsePacket mapFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket) {
        // If a packet is fulfilled, then the Receiver's balance is always adjusted in the outgoing LinkFilter, so
        // there's nothing to do here.

        return interledgerFulfillPacket;
      }

      @Override
      protected InterledgerResponsePacket mapRejectPacket(final InterledgerRejectPacket interledgerRejectPacket) {
        // Only reverse the sender on a reject (The outgoing balance will be untouched).
        try {
          balanceTracker.updateBalanceForReject(
            sourceAccountSettings.getAccountId(), sourcePreparePacket.getAmount().longValue()
          );

          // TODO: Stats
          // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
          // this.stats.incomingDataPacketValue.increment(account, {result :'rejected' },+amount)

        } catch (BalanceTrackerException e) {
          logger.error("RECONCILIATION REQUIRED: Unable to reverse balance update in Redis. " +
            "PreparePacket: {} RejectPacket: {}", sourcePreparePacket, interledgerRejectPacket
          );

          throw e;
        }

        return interledgerRejectPacket;
      }
    }.map(responsePacket);
  }
}
