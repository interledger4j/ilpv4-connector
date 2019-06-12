package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for handling ILP Ping requests. This functionality is defined as a
 * PacketSwitch filter (instead of as an internally-routed account) because units that accrue from this functionality
 * are the Connector's units, whereas units that accrue to an internally-routed account are not the Connector's units,
 * but are instead the property of the owner of the locally-routed account.
 */
public class PingProtocolFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT =
    InterledgerFulfillment.of(Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc="));
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = InterledgerCondition.of(
    Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  private final Supplier<InterledgerAddress> operatorAddressSupplier;

  /**
   * Required-args Constructor.
   */
  public PingProtocolFilter(
    final PacketRejector packetRejector,
    final Supplier<InterledgerAddress> operatorAddressSupplier
  ) {
    super(packetRejector);
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
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

    // TODO: Consider how Ping should work. If we want to allow any account on this Connector to be "ping'd" then
    //  this check should inspect the condition first, and then accept or not. However, if only the Connector itself
    //  can be pinged (current functionality) then gating this on the operatorAddress is more appropriate.
    if (sourcePreparePacket.getDestination().equals(operatorAddressSupplier.get())) {
      if (sourcePreparePacket.getExecutionCondition().equals(PING_PROTOCOL_CONDITION)) {
        return InterledgerFulfillPacket.builder()
          .fulfillment(PING_PROTOCOL_FULFILLMENT)
          .data(sourcePreparePacket.getData())
          .build();
      } else {
        return reject(
          sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F00_BAD_REQUEST,
          "Invalid Ping Protocol Condition"
        );
      }
    } else {
      // Not a ping packet, so continue processing.
      return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
    }
  }
}