package org.interledger.connector.jackson.modules;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link AccountIdModule}.
 */
public class AccountIdModuleTest extends AbstractIdTest {

  private static final AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(ACCOUNT_ID)
      .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final AccountIdContainer actualContainer = objectMapper
      .readValue(json, AccountIdContainer.class);

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    ObjectMapper objectMapperWithoutModule = ObjectMapperFactory.create();
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(ACCOUNT_ID)
      .build();

    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    final AccountIdContainer decodedJson = objectMapperWithoutModule.readValue(actualJson, AccountIdContainer.class);
    assertThat(decodedJson).isEqualTo(expectedContainer);
  }
}
