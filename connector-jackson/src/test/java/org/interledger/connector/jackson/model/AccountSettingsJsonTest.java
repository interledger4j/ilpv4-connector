package org.interledger.connector.jackson.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.link.LinkType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

/**
 * Unit tests for {@link AccountSettings} (de)serialization from/to JSON.
 */
public class AccountSettingsJsonTest {

  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Test
  public void testJsonConversionWithMinimalObject() throws IOException {

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("bob"))
      .createdAt(Instant.MAX)
      .modifiedAt(Instant.MAX)
      .accountRelationship(AccountRelationship.PEER)
      .linkType(LinkType.of("Loopback"))
      .assetCode("USD")
      .assetScale(2)
      .build();

    final String json = objectMapper.writeValueAsString(accountSettings);
    assertThat(json, is("{" +
      "\"accountId\":\"bob\"," +
      "\"createdAt\":\"+1000000000-12-31T23:59:59.999999999Z\"," +
      "\"modifiedAt\":\"+1000000000-12-31T23:59:59.999999999Z\"," +
      "\"description\":\"\"," +
      "\"accountRelationship\":\"PEER\"," +
      "\"assetCode\":\"USD\"," +
      "\"assetScale\":\"2\"," +
      "\"maximumPacketAmount\":null," +
      "\"linkType\":\"LOOPBACK\"," +
      "\"ilpAddressSegment\":\"bob\"," +
      "\"connectionInitiator\":true," +
      "\"internal\":false," +
      "\"sendRoutes\":false," +
      "\"receiveRoutes\":false," +
      "\"balanceSettings\":{" +
      "\"minBalance\":null," +
      "\"settleThreshold\":null," +
      "\"settleTo\":\"0\"" +
      "}," +
      "\"rateLimitSettings\":{" +
      "\"maxPacketsPerSecond\":null" +
      "}," +
      "\"settlementEngineDetails\":null," +
      "\"customSettings\":{}" +
      "}"
    ));

    final AccountSettings deserializedAccountSettings = objectMapper.readValue(json, ImmutableAccountSettings.class);
    assertThat(deserializedAccountSettings, is(accountSettings));
  }

  @Test
  public void testJsonConversionWithFullObject() throws IOException {

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("bob"))
      .description("description")
      .createdAt(Instant.MAX)
      .modifiedAt(Instant.MAX)
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("USD")
      .assetScale(2)
      .maximumPacketAmount(UnsignedLong.ONE)
      .linkType(LinkType.of("Loopback"))
      .ilpAddressSegment("foo")
      .isConnectionInitiator(true)
      .isInternal(true)
      .isSendRoutes(true)
      .isReceiveRoutes(true)
      .balanceSettings(AccountBalanceSettings.builder()
        .minBalance(3L)
        .settleThreshold(9L)
        .settleTo(1L)
        .build())
      .rateLimitSettings(AccountRateLimitSettings.builder()
        .maxPacketsPerSecond(9)
        .build())
      .settlementEngineDetails(SettlementEngineDetails.builder()
        .settlementEngineAccountId(SettlementEngineAccountId.of("settle-id"))
        .baseUrl(HttpUrl.parse("https://example.com"))
        .putCustomSettings("foo", "bar")
        .build())
      .build();

    final String json = objectMapper.writeValueAsString(accountSettings);
    assertThat(json, is("{" +
      "\"accountId\":\"bob\"," +
      "\"createdAt\":\"+1000000000-12-31T23:59:59.999999999Z\"," +
      "\"modifiedAt\":\"+1000000000-12-31T23:59:59.999999999Z\"," +
      "\"description\":\"description\"," +
      "\"accountRelationship\":\"CHILD\"," +
      "\"assetCode\":\"USD\"," +
      "\"assetScale\":\"2\"," +
      "\"maximumPacketAmount\":\"1\"," +
      "\"linkType\":\"LOOPBACK\"," +
      "\"ilpAddressSegment\":\"foo\"," +
      "\"connectionInitiator\":true," +
      "\"internal\":true," +
      "\"sendRoutes\":true," +
      "\"receiveRoutes\":true," +
      "\"balanceSettings\":{" +
      "\"minBalance\":\"3\"," +
      "\"settleThreshold\":\"9\"," +
      "\"settleTo\":\"1\"" +
      "}," +
      "\"rateLimitSettings\":{" +
      "\"maxPacketsPerSecond\":\"9\"" +
      "}," +
      "\"settlementEngineDetails\":{" +
      "\"settlementEngineAccountId\":\"settle-id\"," +
      "\"baseUrl\":\"https://example.com/\"," +
      "\"customSettings\":{\"foo\":\"bar\"}" +
      "}," +
      "\"customSettings\":{}" +
      "}"
    ));

    final AccountSettings deserializedAccountSettings = objectMapper.readValue(json, ImmutableAccountSettings.class);
    assertThat(deserializedAccountSettings, is(accountSettings));
  }

}
