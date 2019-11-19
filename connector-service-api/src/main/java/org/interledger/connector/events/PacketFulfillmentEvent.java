package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import org.immutables.value.Value;

@Value.Immutable
public interface PacketFulfillmentEvent extends ConnectorEvent {

  static ImmutablePacketFulfillmentEvent.Builder builder() {
    return ImmutablePacketFulfillmentEvent.builder();
  }

  InterledgerPreparePacket preparePacket();

  InterledgerFulfillPacket responsePacket();

  AccountSettings destinationAccount();

}
