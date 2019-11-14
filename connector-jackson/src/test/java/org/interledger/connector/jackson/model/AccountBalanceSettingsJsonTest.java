package org.interledger.connector.jackson.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.ImmutableAccountBalanceSettings;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests for {@link AccountBalanceSettings}.
 */
public class AccountBalanceSettingsJsonTest {

  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Test
  public void testJsonConversionWithMinimalObject() throws IOException {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder().build();

    final String json = objectMapper.writeValueAsString(accountBalanceSettings);
    assertThat(json).isEqualTo("{" +
      "\"minBalance\":null," +
      "\"settleThreshold\":null," +
      "\"settleTo\":\"0\"" +
      "}"
    );

    final AccountBalanceSettings deserializedAccountSettings = objectMapper
      .readValue(json, ImmutableAccountBalanceSettings.class);
    assertThat(deserializedAccountSettings).isEqualTo(accountBalanceSettings);
  }

  @Test
  public void testJsonConversionWithFullObject() throws IOException {
    final AccountBalanceSettings accountBalanceSettings = AccountBalanceSettings.builder()
      .minBalance(-200L)
      .settleThreshold(10L)
      .settleTo(1L)
      .build();

    final String json = objectMapper.writeValueAsString(accountBalanceSettings);
    assertThat(json).isEqualTo("{" +
      "\"minBalance\":\"-200\"," +
      "\"settleThreshold\":\"10\"," +
      "\"settleTo\":\"1\"" +
      "}"
    );

    final AccountBalanceSettings deserializedAccountSettings = objectMapper
      .readValue(json, ImmutableAccountBalanceSettings.class);
    assertThat(deserializedAccountSettings).isEqualTo(accountBalanceSettings);
  }

}
