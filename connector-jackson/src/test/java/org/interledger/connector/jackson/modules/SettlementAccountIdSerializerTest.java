package org.interledger.connector.jackson.modules;

import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link SettlementAccountIdSerializer}.
 */
public class SettlementAccountIdSerializerTest extends AbstractIdTest {

  private SettlementEngineAccountId SETTLEMENT_ACCOUNT_ID = SettlementEngineAccountId.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(SETTLEMENT_ACCOUNT_ID);
    assertThat(actual, is("\"" + SETTLEMENT_ACCOUNT_ID.value() + "\""));
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final SettlementAccountIdContainer expectedContainer = ImmutableSettlementAccountIdContainer.builder()
      .settlementAccountId(SETTLEMENT_ACCOUNT_ID)
      .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson, is("{\"settlement_account_id\":\"" + SETTLEMENT_ACCOUNT_ID.value() + "\"}"));
  }
}
