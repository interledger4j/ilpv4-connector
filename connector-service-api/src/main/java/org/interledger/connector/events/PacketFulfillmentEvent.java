package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.links.NextHopInfo;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Immutable
public interface PacketFulfillmentEvent extends ConnectorEvent {

  static ImmutablePacketFulfillmentEvent.Builder builder() {
    return ImmutablePacketFulfillmentEvent.builder();
  }

  InterledgerPreparePacket incomingPreparePacket();

  InterledgerPreparePacket outgoingPreparePacket();

  InterledgerFulfillment fulfillment();

  AccountSettings destinationAccount();

  BigDecimal exchangeRate();

}
