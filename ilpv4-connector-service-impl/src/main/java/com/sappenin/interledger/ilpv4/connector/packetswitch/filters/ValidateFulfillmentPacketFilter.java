package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for validating the fulfillment of an ILP packet.
 */
public class ValidateFulfillmentPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public ValidateFulfillmentPacketFilter(final Supplier<InterledgerAddress> operatorAddressSupplier) {
    super(operatorAddressSupplier);
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
        return reject(
          sourceAccountId, sourcePreparePacket,
          InterledgerErrorCode.F05_WRONG_CONDITION, "Received incorrect fulfillment"
        );
      }
    }

    // If none of the above occur, then return the normal Response.
    return responsePacket;
  }
}
