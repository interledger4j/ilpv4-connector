package org.interledger.connector.events;

import static org.mockito.Mockito.mock;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;

public class DefaultPacketEventPublisherTest {

  private AccountSettings sourceAccountSettings = AccountSettings.builder()
    .accountId(AccountId.of("tim"))
    .assetCode("XRP")
    .assetScale(9)
    .build();

  private AccountSettings nextHopAccountSettings = AccountSettings.builder()
    .accountId(AccountId.of("eric"))
    .assetCode("XRP")
    .assetScale(9)
    .build();

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
    AccountSettings sourceSettings = AccountSettings.builder()
      .accountId(AccountId.of("cassius"))
    publisher.publishRejectionByNextHop();


  }

  @Test
  public void publishRejectionByConnector() {
  }

  @Test
  public void publishFulfillment() {
    BigDecimal fxRate;
    InterledgerRejectPacket rejectPacket;
    publisher.publishRejectionByNextHop(sourceAccountSettings,
      nextHopAccountSettings,
      preparePacket,
      nextHopPacket,
      fxRate,
      rejectPacket);

  }
}