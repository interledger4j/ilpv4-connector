package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import org.immutables.value.Value;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Event that is emitted when the connector sends back an ILP rejection
 */
@Value.Immutable
public interface PacketRejectionEvent extends ConnectorEvent {

  static ImmutablePacketRejectionEvent.Builder builder() {
    return ImmutablePacketRejectionEvent.builder();
  }

  /**
   * Incoming prepare packet from the previous hop
   * @return
   */
  InterledgerPreparePacket incomingPreparePacket();

  /**
   * Outgoing prepare packet to the next hop if (forwarded)
   * @return
   */
  Optional<InterledgerPreparePacket> outgoingPreparePacket();

  /**
   * Reject packet (if prepare rejected)
   * @return
   */
  InterledgerRejectPacket rejection();

  /**
   * Connector account for the next hop (if forwarded)
   * @return
   */
  Optional<AccountSettings> destinationAccount();

  /**
   * Exchange rate that the current connector applied. This is the currency conversion from the incoming prepare
   * packet to the outgoing prepare packet. It is only for this hop and not the conversion for the entire chain
   * of connectors.
   * @return
   */
  Optional<BigDecimal> exchangeRate();

}
