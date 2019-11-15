package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LinkId;
import org.interledger.link.PacketRejector;

import java.util.Objects;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing a maximum packet account for any given ILP packet.
 */
public class MaxPacketAmountFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public MaxPacketAmountFilter(final PacketRejector packetRejector) {
    super(packetRejector);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    Objects.requireNonNull(sourceAccountSettings);
    Objects.requireNonNull(sourcePreparePacket);
    Objects.requireNonNull(filterChain);
    // If the max packet amount is present...
    return sourceAccountSettings.maximumPacketAmount()
      //  if Packet amount is greater-than `maxPacketAmount`, then Reject.
      .filter(maxPacketAmount -> sourcePreparePacket.getAmount().compareTo(maxPacketAmount) > 0)
      .map(maxPacketAmount -> {
        logger.error(
          "Rejecting packet for exceeding max amount. accountId={} maxAmount={} actualAmount={}",
          sourceAccountSettings.accountId(), maxPacketAmount, sourcePreparePacket.getAmount()
        );
        return (InterledgerResponsePacket) packetRejector.reject(
          LinkId.of(sourceAccountSettings.accountId().value()),
          sourcePreparePacket,
          InterledgerErrorCode.F08_AMOUNT_TOO_LARGE,
          String.format(
            "Packet size too large: maxAmount=%s actualAmount=%s", maxPacketAmount,
            sourcePreparePacket.getAmount())
        );
      })
      // Otherwise, the packet amount is fine...
      .orElseGet(() -> filterChain.doFilter(sourceAccountSettings, sourcePreparePacket));
  }
}
