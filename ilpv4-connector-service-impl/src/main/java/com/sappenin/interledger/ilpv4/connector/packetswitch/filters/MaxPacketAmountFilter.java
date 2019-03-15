package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing a maximum packet account for any given ILP packet.
 */
public class MaxPacketAmountFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final AccountManager accountManager;

  public MaxPacketAmountFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier, final AccountManager accountManager
  ) {
    super(operatorAddressSupplier);
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
      .filter(maxPacketAmount -> sourcePreparePacket.getAmount().compareTo(maxPacketAmount) > 0)
      .map(maxPacketAmount -> {
        logger.error(
          "Rejecting packet for exceeding max amount. accountId={} maxAmount={} actualAmount={}",
          sourceAccountId, maxPacketAmount, sourcePreparePacket.getAmount()
        );
        return (InterledgerResponsePacket) InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F03_INVALID_AMOUNT)
          .triggeredBy(operatorAddressSupplier.get())
          .message(String.format("Packet size too large: maxAmount=%s actualAmount=%s", maxPacketAmount,
            sourcePreparePacket.getAmount()))
          .build();
      })
      // Otherwise, the packet amount is fine...
      .orElseGet(() -> filterChain.doFilter(sourceAccountId, sourcePreparePacket));
  }
}
