package org.interledger.connector.jackson.modules;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit tests for {@link AccountIdDeserializer}.
 */
public class AccountIdDeserializerTest extends AbstractIdTest {

  private AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID().toString());

  @Test
  public void shouldDeserialize() throws IOException {
    final AccountId actual = objectMapper
      .readValue("\"" + ACCOUNT_ID.value() + "\"", AccountId.class);

    assertThat(actual).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(ACCOUNT_ID)
      .build();

    final AccountIdContainer actualContainer = objectMapper.readValue(
      "{\"account_id\":\"" + ACCOUNT_ID.value() + "\"}",
      AccountIdContainer.class
    );

    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

}
