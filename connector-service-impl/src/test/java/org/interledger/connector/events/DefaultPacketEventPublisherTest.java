package org.interledger.connector.events;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.http.IlpOverHttpLink;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;

public class DefaultPacketEventPublisherTest {

  private AccountSettings sourceAccountSettings = newAccountSettings("tim");

  private AccountSettings nextHopAccountSettings = newAccountSettings("eric");

  private InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
    .expiresAt(Instant.now())
    .destination(InterledgerAddress.of("g.destination"))
    .amount(UnsignedLong.valueOf(99))
    .executionCondition(InterledgerConstants.ALL_ZEROS_FULFILLMENT.getCondition())
    .build();

  private InterledgerPreparePacket nextHopPacket = InterledgerPreparePacket.builder()
    .from(preparePacket)
    .amount(UnsignedLong.valueOf(98))
    .expiresAt(preparePacket.getExpiresAt().minusSeconds(1))
    .build();

  private InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
    .message("what's a fish")
    .triggeredBy(InterledgerAddress.of("g.trigger"))
    .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
    .data(new byte[32])
    .build();

  private InterledgerFulfillment fulfillment = InterledgerConstants.ALL_ZEROS_FULFILLMENT;

  private BigDecimal fxRate = new BigDecimal("1.23");

  private EventBus eventBus;
  private DefaultPacketEventPublisher publisher;

  @Before
  public void setUp() {
    eventBus = mock(EventBus.class);
    publisher = new DefaultPacketEventPublisher(eventBus);
  }

  @Test
  public void publishRejectionByNextHop() {
    publisher.publishRejectionByNextHop(sourceAccountSettings,
      nextHopAccountSettings,
      preparePacket,
      nextHopPacket,
      fxRate,
      rejectPacket);

    verify(eventBus).post(PacketRejectionEvent.builder()
      .accountSettings(sourceAccountSettings)
      .destinationAccount(nextHopAccountSettings)
      .exchangeRate(fxRate)
      .incomingPreparePacket(preparePacket)
      .outgoingPreparePacket(nextHopPacket)
      .rejection(rejectPacket)
      .message(rejectPacket.getMessage())
      .build()
    );
  }

  @Test
  public void publishRejectionByConnector() {
    publisher.publishRejectionByConnector(sourceAccountSettings, preparePacket, rejectPacket);

    verify(eventBus).post(PacketRejectionEvent.builder()
      .accountSettings(sourceAccountSettings)
      .incomingPreparePacket(preparePacket)
      .rejection(rejectPacket)
      .message(rejectPacket.getMessage())
      .build()
    );
  }

  @Test
  public void publishFulfillment() {
    publisher.publishFulfillment(sourceAccountSettings,
      nextHopAccountSettings,
      preparePacket,
      nextHopPacket,
      fxRate,
      fulfillment);

    verify(eventBus).post(PacketFullfillmentEvent.builder()
      .accountSettings(sourceAccountSettings)
      .destinationAccount(nextHopAccountSettings)
      .outgoingPreparePacket(nextHopPacket)
      .incomingPreparePacket(preparePacket)
      .exchangeRate(fxRate)
      .fulfillment(fulfillment)
      .message("Fulfilled successfully")
      .build()
    );
  }

  private ImmutableAccountSettings newAccountSettings(String accountId) {
    return AccountSettings.builder()
      .accountId(AccountId.of(accountId))
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetCode("XRP")
      .assetScale(9)
      .build();
  }

}