package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.packetswitch.InterledgerAddressUtils;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} that enforces allowed-destination logic, preventing a particular
 * account from sending to disallowed destinations (e.g., internally routed addresses like `self.foo`).
 */
public class AllowedDestinationPacketFilter implements PacketSwitchFilter {

  private static final String DESTINATION_ADDRESS_IS_UNREACHABLE = "Destination address is unreachable";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<InterledgerAddress> operatorAddressSupplier;
  private final InterledgerAddressUtils addressUtils;

  public AllowedDestinationPacketFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier, final InterledgerAddressUtils addressUtils) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.addressUtils = Objects.requireNonNull(addressUtils);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    // Before packet-forwarding is engaged, this code ensures the incoming account/packet information is eligible
    // to be packet-switched, considering the destination address as well as characteristics of the source account.
    if (
      !addressUtils.isDestinationAllowedFromAccount(sourceAccountId, sourcePreparePacket.getDestination())
    ) {
      logger.error(
        "AccountId `{}` is not allowed to send to destination address `{}`",
        sourceAccountId, sourcePreparePacket.getDestination()
      );
      // REJECT!
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F02_UNREACHABLE)
        .triggeredBy(operatorAddressSupplier.get())
        .message(DESTINATION_ADDRESS_IS_UNREACHABLE)
        .build();
    } else {
      return filterChain.doFilter(sourceAccountId, sourcePreparePacket);
    }
  }
}
