package org.interledger.connector.jackson.modules;

import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link SettlementAccountIdDeserializer}.
 */
public class SettlementAccountIdDeserializerTest extends AbstractIdTest {

  private SettlementEngineAccountId SETTLEMENT_ACCOUNT_ID = SettlementEngineAccountId.of(UUID.randomUUID().toString());

  @Test
  public void shouldDeserialize() throws IOException {
    final SettlementEngineAccountId actual = objectMapper
      .readValue("\"" + SETTLEMENT_ACCOUNT_ID.value() + "\"", SettlementEngineAccountId.class);

    assertThat(actual, is(SETTLEMENT_ACCOUNT_ID));
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final SettlementAccountIdContainer expectedContainer = ImmutableSettlementAccountIdContainer.builder()
      .settlementAccountId(SETTLEMENT_ACCOUNT_ID)
      .build();

    final SettlementAccountIdContainer actualContainer = objectMapper.readValue(
      "{\"settlement_account_id\":\"" + SETTLEMENT_ACCOUNT_ID.value() + "\"}",
      SettlementAccountIdContainer.class
    );

    assertThat(actualContainer, is(expectedContainer));
  }

}
