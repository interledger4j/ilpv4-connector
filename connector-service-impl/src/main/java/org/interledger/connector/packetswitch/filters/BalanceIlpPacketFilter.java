package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.BalanceTrackerException;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LinkId;
import org.interledger.link.PacketRejector;

import java.util.Objects;

/**
 * An implementation of {@link PacketSwitchFilter} for handling balance updates for a given ILP request/response flow.
 */
public class BalanceIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final BalanceTracker balanceTracker;

  public BalanceIlpPacketFilter(final PacketRejector packetRejector, final BalanceTracker balanceTracker) {
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
          sourceAccountSettings.accountId(), sourcePreparePacket.getAmount().longValue(),
          sourceAccountSettings.balanceSettings().minBalance()
      );

      // TODO: Stats (Should half of this instead be in the balance tracker?)
      // this.stats.balance.setValue(account, {}, balance.getValue().toNumber())
      // this.stats.incomingDataPacketValue.increment(account, {result :'rejected' },+amount)

    } catch (BalanceTrackerException e) {
      // If there's an error, it means the prepare balance update was not applied, so simply log the exception and
      // reject.
      logger.error(e.getMessage(), e);
      return packetRejector.reject(
          LinkId.of(sourceAccountSettings.accountId().value()),
          sourcePreparePacket,
          InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY, ""
      );
    }

    return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket).map(
        //////////////////////
        // If FulfillPacket...
        //////////////////////
        (interledgerFulfillPacket) -> {
          // If a packet is fulfilled, then the Receiver's balance is always adjusted in the outgoing LinkFilter, so
          // there's nothing to do here.
          return interledgerFulfillPacket;
        },
        //////////////////////
        // If Reject Packet...
        //////////////////////
        (interledgerRejectPacket) -> {
          // Only reverse the sender on a reject (The outgoing balance will be untouched).
          try {
            balanceTracker.updateBalanceForReject(
                sourceAccountSettings.accountId(), sourcePreparePacket.getAmount().longValue()
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
    );
  }
}
