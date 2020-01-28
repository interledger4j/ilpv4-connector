package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import com.google.common.eventbus.EventBus;

import java.math.BigDecimal;

public class DefaultPacketEventPublisher implements PacketEventPublisher {

  private final EventBus eventBus;

  public DefaultPacketEventPublisher(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void publishRejectionByNextHop(AccountSettings sourceAccountSettings,
                                        AccountSettings nextHopAccountSettings,
                                        InterledgerPreparePacket preparePacket,
                                        InterledgerPreparePacket nextHopPacket,
                                        BigDecimal fxRate,
                                        InterledgerRejectPacket rejectPacket) {
    eventBus.post(PacketRejectionEvent.builder()
      .accountSettings(sourceAccountSettings)
      .destinationAccount(nextHopAccountSettings)
      .exchangeRate(fxRate)
      .incomingPreparePacket(preparePacket)
      .outgoingPreparePacket(nextHopPacket)
      .message(rejectPacket.getMessage())
      .rejection(rejectPacket)
      .build());
  }

  @Override
  public void publishRejectionByConnector(AccountSettings sourceAccountSettings,
                                          InterledgerPreparePacket preparePacket,
                                          InterledgerRejectPacket rejectPacket) {
    eventBus.post(PacketRejectionEvent.builder()
      .accountSettings(sourceAccountSettings)
      .incomingPreparePacket(preparePacket)
      .message(rejectPacket.getMessage())
      .rejection(rejectPacket)
      .build());
  }

  @Override
  public void publishFulfillment(AccountSettings sourceAccountSettings,
                                 AccountSettings nextHopAccountSettings,
                                 InterledgerPreparePacket preparePacket,
                                 InterledgerPreparePacket nextHopPacket,
                                 BigDecimal fxRate,
                                 InterledgerFulfillment fulfillment) {
    eventBus.post(PacketFullfillmentEvent.builder()
      .accountSettings(sourceAccountSettings)
      .destinationAccount(nextHopAccountSettings)
      .exchangeRate(fxRate)
      .incomingPreparePacket(preparePacket)
      .outgoingPreparePacket(nextHopPacket)
      .fulfillment(fulfillment)
      .message("Fulfilled successfully")
      .build());
  }
}
