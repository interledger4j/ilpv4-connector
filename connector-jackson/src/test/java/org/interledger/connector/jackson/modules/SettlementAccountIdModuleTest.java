package org.interledger.connector.jackson.modules;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link SettlementAccountIdModule}.
 */
public class SettlementAccountIdModuleTest extends AbstractIdTest {

  private static final SettlementEngineAccountId SETTLEMENT_ACCOUNT_ID =
    SettlementEngineAccountId.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final SettlementAccountIdContainer expectedContainer = ImmutableSettlementAccountIdContainer.builder()
      .settlementAccountId(SETTLEMENT_ACCOUNT_ID)
      .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final SettlementAccountIdContainer actualContainer = objectMapper
      .readValue(json, SettlementAccountIdContainer.class);

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = ObjectMapperFactory.create();
    final SettlementAccountIdContainer expectedContainer = ImmutableSettlementAccountIdContainer.builder()
      .settlementAccountId(SETTLEMENT_ACCOUNT_ID)
      .build();

    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    final SettlementAccountIdContainer decodedJson =
      objectMapperWithoutModule.readValue(actualJson, SettlementAccountIdContainer.class);
    assertThat(decodedJson).isEqualTo(expectedContainer);
  }
}
