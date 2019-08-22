package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * An implementation of {@link PacketSwitchFilter} for validating the fulfillment of an ILP packet.
 */
public class ValidateFulfillmentPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public ValidateFulfillmentPacketFilter(final PacketRejector packetRejector) {
    super(packetRejector);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    final InterledgerResponsePacket responsePacket =
      filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);

    // Only for a fulfill...
    if (InterledgerFulfillPacket.class.isAssignableFrom(responsePacket.getClass())) {
      final InterledgerFulfillPacket fulfillPacket = (InterledgerFulfillPacket) responsePacket;
      if (!fulfillPacket.getFulfillment().validateCondition(sourcePreparePacket.getExecutionCondition())) {
        logger.error(
          "Received incorrect fulfillment from account. " +
            "accountId=`{}` fulfillment=`{}` calculatedCondition=`{}` executionCondition=`{}`",
          sourceAccountSettings,
          fulfillPacket.getFulfillment(),
          fulfillPacket.getFulfillment().getCondition(),
          sourcePreparePacket.getExecutionCondition()
        );
        return packetRejector.reject(
          sourceAccountSettings.getAccountId(), sourcePreparePacket,
          InterledgerErrorCode.F05_WRONG_CONDITION, "Received incorrect fulfillment"
        );
      }
    }

    // If none of the above occur, then return the normal Response.
    return responsePacket;
  }
}
