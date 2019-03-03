package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for validating the fulfillment of an ILP packet.
 */
public class ValidateFulfillmentPacketFilter implements PacketSwitchFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Supplier<InterledgerAddress> operatorAddressSupplier;

  public ValidateFulfillmentPacketFilter(final Supplier<InterledgerAddress> operatorAddressSupplier) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    final InterledgerResponsePacket responsePacket =
      filterChain.doFilter(sourceAccountId, sourcePreparePacket);

    // Only for a fulfill...
    if (InterledgerFulfillPacket.class.isAssignableFrom(responsePacket.getClass())) {
      final InterledgerFulfillPacket fulfillPacket = (InterledgerFulfillPacket) responsePacket;
      if (!fulfillPacket.getFulfillment().validateCondition(sourcePreparePacket.getExecutionCondition())) {
        logger.error(
          "Received incorrect fulfillment from account. " +
            "accountId=`{}` fulfillment=`{}` calculatedCondition=`{}` executionCondition=`{}`",
          sourceAccountId,
          fulfillPacket.getFulfillment(),
          fulfillPacket.getFulfillment().getCondition(),
          sourcePreparePacket.getExecutionCondition()
        );
        return InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F05_WRONG_CONDITION)
          .triggeredBy(operatorAddressSupplier.get())
          .message("Received incorrect fulfillment").build();
      }
    }

    // If none of the above occur, then return the normal Response.
    return responsePacket;
  }
}
