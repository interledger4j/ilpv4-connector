package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing a maximum packet account for any given ILP packet.
 */
public class MaxPacketAmountFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final AccountManager accountManager;

  public MaxPacketAmountFilter(final PacketRejector packetRejector, final AccountManager accountManager) {
    super(packetRejector);
    this.accountManager = Objects.requireNonNull(accountManager);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    final Account account = accountManager.getAccount(sourceAccountId)
      // REJECT due to no account...
      .orElseThrow(() -> new InterledgerProtocolException(
        reject(sourceAccountId, sourcePreparePacket, InterledgerErrorCode.T00_INTERNAL_ERROR,
          String.format("No Account found: `%s`", sourceAccountId))));

    // If the max packet amount is present...
    return account.getAccountSettings().getMaximumPacketAmount()
      //  if Packet amount is greater-than `maxPacketAmount`, then Reject.
      .filter(maxPacketAmount -> sourcePreparePacket.getAmount().compareTo(maxPacketAmount) >= 0)
      .map(maxPacketAmount -> {
        logger.error(
          "Rejecting packet for exceeding max amount. accountId={} maxAmount={} actualAmount={}",
          sourceAccountId, maxPacketAmount, sourcePreparePacket.getAmount()
        );
        return (InterledgerResponsePacket) reject(
          sourceAccountId, sourcePreparePacket, InterledgerErrorCode.F08_AMOUNT_TOO_LARGE,
          String.format(
            "Packet size too large: maxAmount=%s actualAmount=%s", maxPacketAmount, sourcePreparePacket.getAmount())
        );
      })
      // Otherwise, the packet amount is fine...
      .orElseGet(() -> filterChain.doFilter(sourceAccountId, sourcePreparePacket));
  }
}
