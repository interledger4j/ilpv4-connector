package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import java.math.BigDecimal;

public interface PacketEventPublisher {

  void publishRejectionByNextHop(AccountSettings sourceAccountSettings,
                                 AccountSettings nextHopAccountSettings,
                                 InterledgerPreparePacket preparePacket,
                                 InterledgerPreparePacket nextHopPacket,
                                 BigDecimal fxRate,
                                 InterledgerRejectPacket rejectPacket);

  void publishRejectionByConnector(AccountSettings sourceAccountSettings,
                                   InterledgerPreparePacket preparePacket,
                                   InterledgerRejectPacket rejectPacket);

  void publishFulfillment(AccountSettings sourceAccountSettings,
                          AccountSettings nextHopAccountSettings,
                          InterledgerPreparePacket preparePacket,
                          InterledgerPreparePacket nextHopPacket,
                          BigDecimal fxRate,
                          InterledgerFulfillment fulfillment);
}
