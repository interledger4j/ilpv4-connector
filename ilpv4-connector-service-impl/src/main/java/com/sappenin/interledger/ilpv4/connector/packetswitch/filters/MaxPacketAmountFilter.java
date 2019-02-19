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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing a maximum packet account for any given ILP packet.
 */
public class MaxPacketAmountFilter implements PacketSwitchFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Supplier<InterledgerAddress> operatorAddressSupplier;

  private final AccountManager accountManager;

  public MaxPacketAmountFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier, final AccountManager accountManager
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.accountManager = Objects.requireNonNull(accountManager);
  }

  @Override
  public Optional<InterledgerResponsePacket> doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    final Account account = accountManager.getAccount(sourceAccountId)
      .orElseThrow(() ->
        // REJECT due to no account...
        new InterledgerProtocolException(InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
          .triggeredBy(operatorAddressSupplier.get())
          .message(String.format("No Account found!"))
          .build())
      );

    // If the max packet amount is present...
    return account.getAccountSettings().getMaximumPacketAmount()
      //  if Packet amount is greater-than `maxPacketAmount`, then Reject.
      .filter(maxPacketAmount -> sourcePreparePacket.getAmount().compareTo(maxPacketAmount) > 0)
      .map(maxPacketAmount -> {
        logger.error(
          "Rejecting packet for exceeding max amount. accountId={} maxAmount={} actualAmount={}",
          sourceAccountId, maxPacketAmount, sourcePreparePacket.getAmount()
        );
        return Optional.<InterledgerResponsePacket>of(InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
          .triggeredBy(operatorAddressSupplier.get())
          .message(String.format("Packet size too large: maxAmount=%s actualAmount=%s", maxPacketAmount,
            sourcePreparePacket.getAmount()))
          .build());
      })
      // Otherwise, the packet amount is fine...
      .orElseGet(() -> filterChain.doFilter(sourceAccountId, sourcePreparePacket));
  }
}
