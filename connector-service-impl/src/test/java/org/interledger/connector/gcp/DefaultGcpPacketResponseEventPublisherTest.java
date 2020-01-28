package org.interledger.connector.gcp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.events.PacketFullfillmentEvent;
import org.interledger.connector.events.PacketRejectionEvent;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.http.IlpOverHttpLink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultGcpPacketResponseEventPublisherTest {
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.hugh.honey");
  private static final InterledgerAddress DEST_ADDRESS = InterledgerAddress.of("test.vic.vinegar");
  public static final String FULFILLS = "fulfills";
  public static final String REJECTS = "rejects";

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private DefaultGcpPacketResponseEventPublisher publisher;
  @Mock
  private PubSubTemplate template;

  // mock out time to make it easier to assert the expected JSON
  private int nanos = (int) TimeUnit.NANOSECONDS.convert(678, TimeUnit.MILLISECONDS);
  private Clock clock = Clock.fixed(LocalDateTime.of(2000, 1, 2, 3, 4, 5, nanos).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
  private ObjectMapper mapper = ObjectMapperFactory.create();

  @Before
  public void setUp() {
    publisher = new DefaultGcpPacketResponseEventPublisher(template,
      Optional.of(FULFILLS),
      Optional.of(REJECTS),
      OPERATOR_ADDRESS,
      mapper,
      clock);
  }

  @Test
  public void publishFulfillment() {
    AccountSettings incomingAccount = AccountSettings.builder()
      .accountId(AccountId.of("hugh_honey"))
      .accountRelationship(AccountRelationship.PEER)
      .assetScale(9)
      .assetCode("XRP")
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .build();

    AccountSettings destinationAccount = AccountSettings.builder()
      .accountId(AccountId.of("vic_vinegar"))
      .accountRelationship(AccountRelationship.PEER)
      .assetScale(2)
      .assetCode("USD")
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .build();

    InterledgerFulfillment fulfillment = InterledgerFulfillment.of(new byte[32]);
    InterledgerPreparePacket incomingPreparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(UnsignedLong.valueOf(100))
      .destination(DEST_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(1))
      .build();

    InterledgerPreparePacket outgoingPreparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(UnsignedLong.valueOf(123))
      .destination(DEST_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(2))
      .build();

    PacketFullfillmentEvent packetFulfillmentEvent = PacketFullfillmentEvent.builder()
      .exchangeRate(new BigDecimal("1.23"))
      .destinationAccount(destinationAccount)
      .fulfillment(fulfillment)
      .accountSettings(incomingAccount)
      .incomingPreparePacket(incomingPreparePacket)
      .outgoingPreparePacket(outgoingPreparePacket)
      .message("hello")
      .build();

    publisher.publish(packetFulfillmentEvent);

    String expected = "{\"prevHopAccount\":\"hugh_honey\"," +
      "\"prevHopAssetCode\":\"XRP\"," +
      "\"prevHopAmount\":\"100\"," +
      "\"nextHopAccount\":\"vic_vinegar\"," +
      "\"nextHopAssetCode\":\"USD\"," +
      "\"nextHopAmount\":\"123\"," +
      "\"spread\":\"0\"," +
      "\"exchangeRate\":\"1.23\"," +
      "\"connectorIlpAddress\":\"test.hugh.honey\"," +
      "\"destinationIlpAddress\":\"test.vic.vinegar\"," +
      "\"fulfillment\":\"Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=\"," +
      "\"timestamp\":\"2000-01-02T03:04:05.678Z\"," +
      "\"prevHopAssetScale\":\"9\"," +
      "\"nextHopAssetScale\":\"2\"," +
      "\"status\":\"FULFILLED\"}";

    verify(template).publish(FULFILLS, expected);
  }

  @Test
  public void publishFulfillmentSkippedIfNotConfigured() {
    publisher = new DefaultGcpPacketResponseEventPublisher(template,
      Optional.empty(),
      Optional.of(REJECTS),
      OPERATOR_ADDRESS,
      mapper,
      clock);
    publisher.publish(mock(PacketFullfillmentEvent.class));
    verifyNoInteractions(template);
  }

  @Test
  public void publishRejectionFromNextHop() {
    AccountSettings incomingAccount = AccountSettings.builder()
      .accountId(AccountId.of("hugh_honey"))
      .accountRelationship(AccountRelationship.PEER)
      .assetScale(9)
      .assetCode("XRP")
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .build();

    AccountSettings destinationAccount = AccountSettings.builder()
      .accountId(AccountId.of("vic_vinegar"))
      .accountRelationship(AccountRelationship.PEER)
      .assetScale(2)
      .assetCode("USD")
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .build();

    InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F05_WRONG_CONDITION)
      .triggeredBy(DEST_ADDRESS)
      .message("Epstein didn't kill himself")
      .build();

    InterledgerPreparePacket incomingPreparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(UnsignedLong.valueOf(100))
      .destination(DEST_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(1))
      .build();

    InterledgerPreparePacket outgoingPreparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(UnsignedLong.valueOf(123))
      .destination(DEST_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(2))
      .build();

    PacketRejectionEvent packetRejectionEvent = PacketRejectionEvent.builder()
      .exchangeRate(new BigDecimal("1.23"))
      .destinationAccount(destinationAccount)
      .rejection(rejectPacket)
      .accountSettings(incomingAccount)
      .incomingPreparePacket(incomingPreparePacket)
      .outgoingPreparePacket(outgoingPreparePacket)
      .message("event message")
      .build();

    publisher.publish(packetRejectionEvent);

    String expected = "{\"prevHopAccount\":\"hugh_honey\"," +
      "\"prevHopAssetCode\":\"XRP\"," +
      "\"prevHopAmount\":\"100\"," +
      "\"nextHopAccount\":\"vic_vinegar\"," +
      "\"nextHopAssetCode\":\"USD\"," +
      "\"nextHopAmount\":\"123\"," +
      "\"spread\":\"0\"," +
      "\"exchangeRate\":\"1.23\"," +
      "\"connectorIlpAddress\":\"test.hugh.honey\"," +
      "\"destinationIlpAddress\":\"test.vic.vinegar\"," +
      "\"timestamp\":\"2000-01-02T03:04:05.678Z\"," +
      "\"prevHopAssetScale\":\"9\"," +
      "\"nextHopAssetScale\":\"2\"," +
      "\"status\":\"REJECTED\"," +
      "\"rejectionMessage\":\"Epstein didn't kill himself\"," +
      "\"rejectionCode\":\"F05\"," +
      "\"rejectionTriggeredBy\":\"test.vic.vinegar\"}";

    verify(template).publish(REJECTS, expected);
  }

  @Test
  public void publishRejectionFromCurrentHop() {
    AccountSettings incomingAccount = AccountSettings.builder()
      .accountId(AccountId.of("hugh_honey"))
      .accountRelationship(AccountRelationship.PEER)
      .assetScale(9)
      .assetCode("XRP")
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .build();

    InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F05_WRONG_CONDITION)
      .triggeredBy(DEST_ADDRESS)
      .message("Epstein didn't kill himself")
      .build();

    InterledgerPreparePacket incomingPreparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(UnsignedLong.valueOf(100))
      .destination(DEST_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(1))
      .build();

    PacketRejectionEvent packetRejectionEvent = PacketRejectionEvent.builder()
      .rejection(rejectPacket)
      .accountSettings(incomingAccount)
      .incomingPreparePacket(incomingPreparePacket)
      .message("event message")
      .build();

    publisher.publish(packetRejectionEvent);

    String expected = "{\"prevHopAccount\":\"hugh_honey\"," +
      "\"prevHopAssetCode\":\"XRP\"," +
      "\"prevHopAmount\":\"100\"," +
      "\"spread\":\"0\"," +
      "\"connectorIlpAddress\":\"test.hugh.honey\"," +
      "\"destinationIlpAddress\":\"test.vic.vinegar\"," +
      "\"timestamp\":\"2000-01-02T03:04:05.678Z\"," +
      "\"prevHopAssetScale\":\"9\"," +
      "\"status\":\"REJECTED\"," +
      "\"rejectionMessage\":\"Epstein didn't kill himself\"," +
      "\"rejectionCode\":\"F05\"," +
      "\"rejectionTriggeredBy\":\"test.vic.vinegar\"}";

    verify(template).publish(REJECTS, expected);
  }

  @Test
  public void publishRejectionSkippedIfNotConfigured() {
    publisher = new DefaultGcpPacketResponseEventPublisher(template,
      Optional.of(FULFILLS),
      Optional.empty(),
      OPERATOR_ADDRESS,
      mapper,
      clock);
    publisher.publish(mock(PacketRejectionEvent.class));
    verifyNoInteractions(template);
  }

}