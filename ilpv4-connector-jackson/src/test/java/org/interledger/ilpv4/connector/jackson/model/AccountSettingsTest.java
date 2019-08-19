package org.interledger.ilpv4.connector.jackson.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link AccountSettings}.
 */
public class AccountSettingsTest {

  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Test
  public void testJsonConversionWithMinimalObject() throws IOException {

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("bob"))
      .accountRelationship(AccountRelationship.PEER)
      .linkType(LinkType.of("Loopback"))
      .assetCode("USD")
      .assetScale(2)
      .build();

    final String json = objectMapper.writeValueAsString(accountSettings);
    assertThat(json, is("{" +
      "\"accountId\":\"bob\"," +
      "\"linkType\":\"LOOPBACK\"," +
      "\"ilpAddressSegment\":null," +
      "\"accountRelationship\":\"PEER\"," +
      "\"assetCode\":\"USD\"," +
      "\"assetScale\":\"2\"," +
      "\"maximumPacketAmount\":null," +
      "\"customSettings\":{}," +
      "\"description\":\"\"," +
      "\"balanceSettings\":{" +
      "\"minBalance\":null," +
      "\"settleThreshold\":null," +
      "\"settleTo\":\"0\"" +
      "}," +
      "\"rateLimitSettings\":{" +
      "\"maxPacketsPerSecond\":null" +
      "}," +
      "\"isConnectionInitiator\":false," +
      "\"isInternal\":false," +
      "\"isSendRoutes\":false," +
      "\"isReceiveRoutes\":false," +
      "\"settlementEngineDetails\":null" +
      "}"
    ));

    final AccountSettings deserializedAccountSettings = objectMapper.readValue(json, ImmutableAccountSettings.class);
    assertThat(deserializedAccountSettings, is(accountSettings));
  }

}
