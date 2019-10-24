package org.interledger.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.function.Supplier;

public class OutgoingMaxPacketAmountLinkFilter extends AbstractLinkFilter implements LinkFilter {

  public OutgoingMaxPacketAmountLinkFilter(Supplier<InterledgerAddress> operatorAddressSupplier) {
    super(operatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
      final AccountSettings destinationAccountSettings,
      final InterledgerPreparePacket destPreparePacket,
      final LinkFilterChain filterChain
  ) {
    Objects.requireNonNull(destinationAccountSettings);
    Objects.requireNonNull(destPreparePacket);
    Objects.requireNonNull(filterChain);
    // If the max packet amount is present...
    return destinationAccountSettings.maximumPacketAmount()
        //  if Packet amount is greater-than `maxPacketAmount`, then Reject.
        .filter(maxPacketAmount -> destPreparePacket.getAmount().longValue() > maxPacketAmount)
        .map(maxPacketAmount -> {
          logger.error(
              "Rejecting packet for exceeding max amount. accountId={} maxAmount={} actualAmount={}",
              destinationAccountSettings.accountId(), maxPacketAmount, destPreparePacket.getAmount()
          );
          return (InterledgerResponsePacket) reject(
              destinationAccountSettings.accountId(), destPreparePacket, InterledgerErrorCode.F08_AMOUNT_TOO_LARGE,
              String.format(
                  "Packet size too large: maxAmount=%s actualAmount=%s", maxPacketAmount, destPreparePacket.getAmount())
          );
        })
        // Otherwise, the packet amount is fine...
        .orElseGet(() -> filterChain.doFilter(destinationAccountSettings, destPreparePacket));
  }
}
