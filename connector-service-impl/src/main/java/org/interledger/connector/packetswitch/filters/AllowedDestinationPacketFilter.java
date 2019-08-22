package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.packetswitch.InterledgerAddressUtils;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;


/**
 * An implementation of {@link PacketSwitchFilter} that enforces allowed-destination logic, preventing a particular
 * account from sending to disallowed destinations (e.g., internally routed addresses like `self.foo`).
 */
public class AllowedDestinationPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private static final String DESTINATION_ADDRESS_IS_UNREACHABLE = "Destination address is unreachable";

  private final InterledgerAddressUtils addressUtils;

  public AllowedDestinationPacketFilter(
    final PacketRejector packetRejector, final InterledgerAddressUtils addressUtils
  ) {
    super(packetRejector);
    this.addressUtils = Objects.requireNonNull(addressUtils);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    // Before packet-forwarding is engaged, this code ensures the incoming account/packet information is eligible
    // to be packet-switched, considering the destination address as well as characteristics of the source account.
    if (
      !addressUtils.isDestinationAllowedFromAccount(
        sourceAccountSettings.getAccountId(), sourcePreparePacket.getDestination()
      )
    ) {
      logger.error(
        "AccountId `{}` is not allowed to send to destination address `{}`",
        sourceAccountSettings.getAccountId().value(), sourcePreparePacket.getDestination().getValue()
      );
      // REJECT!
      return packetRejector.reject(
        sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F02_UNREACHABLE,
        DESTINATION_ADDRESS_IS_UNREACHABLE
      );
    } else {
      return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
    }
  }
}
