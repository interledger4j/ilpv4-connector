package org.interledger.connector.gcp;

import static org.mockito.Mockito.verify;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
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
import java.util.concurrent.TimeUnit;

public class DefaultFulfillmentPublisherTest {
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.hugh.honey");
  private static final InterledgerAddress DEST_ADDRESS = InterledgerAddress.of("test.vic.vinegar");

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  public static final String TOPIC = "real-estate";
  private DefaultFulfillmentPublisher publisher;
  @Mock
  private PubSubTemplate template;

  private Clock clock;

  @Before
  public void setUp() {
    // mock out time to make it easier to assert the expected JSON
    int nanos = (int) TimeUnit.NANOSECONDS.convert(678, TimeUnit.MILLISECONDS);
    clock = Clock.fixed(LocalDateTime.of(2000, 1, 2, 3, 4, 5, nanos).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    ObjectMapper mapper = ObjectMapperFactory.create();
    publisher = new DefaultFulfillmentPublisher(template, TOPIC, OPERATOR_ADDRESS, mapper, clock);
  }

  @Test
  public void publish() {
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
      .destination(OPERATOR_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(1))
      .build();

    InterledgerPreparePacket outgoingPreparePacket = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerCondition.of(new byte[32]))
      .amount(UnsignedLong.valueOf(123))
      .destination(DEST_ADDRESS)
      .expiresAt(Instant.now().plusSeconds(2))
      .build();

    PacketFulfillmentEvent packetFulfillmentEvent = PacketFulfillmentEvent.builder()
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
      "\"nextHopAssetScale\":\"2\"}";

    verify(template).publish(TOPIC, expected);
  }

}