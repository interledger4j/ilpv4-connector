package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;

import org.immutables.value.Value;

import java.math.BigDecimal;

/**
 * Event that is emitted when the connector receives a ILP fulfillment
 */
@Value.Immutable
public interface PacketFulfillmentEvent extends ConnectorEvent {

  static ImmutablePacketFulfillmentEvent.Builder builder() {
    return ImmutablePacketFulfillmentEvent.builder();
  }

  /**
   * Incoming prepare packet from the previous hop
   * @return
   */
  InterledgerPreparePacket incomingPreparePacket();

  /**
   * Outgoing prepare packet to the next hop
   * @return
   */
  InterledgerPreparePacket outgoingPreparePacket();

  /**
   * Fulfillment condition from the receiver
   * @return
   */
  InterledgerFulfillment fulfillment();

  /**
   * Connector account for the next hop
   * @return
   */
  AccountSettings destinationAccount();

  /**
   * Exchange rate that the current connector applied. This is the currency conversion from the incoming prepare
   * packet to the outgoing prepare packet. It is only for this hop and not the conversion for the entire chain
   * of connectors.
   * @return
   */
  BigDecimal exchangeRate();

}
