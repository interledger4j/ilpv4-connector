package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for handling ILP Ping requests. This functionality is defined as a
 * PacketSwitch filter (instead of as an internally-routed account) because units that accrue from this functionality
 * are the Connector's units, whereas units that accrue to an internally-routed account are not the Connector's units,
 * but are instead the property of the owner of the locally-routed account.
 */
public class PingProtocolFilter implements PacketSwitchFilter {

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT =
    InterledgerFulfillment.of(Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc="));
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = InterledgerCondition.of(
    Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  private final Supplier<InterledgerAddress> nodeOperatorAddressSupplier;

  /**
   * Required-args Constructor.
   */
  public PingProtocolFilter(final Supplier<InterledgerAddress> nodeOperatorAddressSupplier) {
    this.nodeOperatorAddressSupplier = Objects.requireNonNull(nodeOperatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);
    Objects.requireNonNull(filterChain);

    if (sourcePreparePacket.getExecutionCondition().equals(PING_PROTOCOL_CONDITION)) {
      return InterledgerFulfillPacket.builder()
        .fulfillment(PING_PROTOCOL_FULFILLMENT)
        .data(sourcePreparePacket.getData())
        .build();
    } else {
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .message("Invalid Ping Protocol Condition")
        .triggeredBy(nodeOperatorAddressSupplier.get())
        .build();
    }


  }

}
