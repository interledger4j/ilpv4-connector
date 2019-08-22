package org.interledger.ilpv4.connector.jackson.modules;

import org.interledger.connector.accounts.AccountId;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link AccountIdSerializer}.
 */
public class AccountIdSerializerTest extends AbstractIdTest {

  private AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID().toString());

  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(ACCOUNT_ID);
    assertThat(actual, is("\"" + ACCOUNT_ID.value() + "\""));
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(ACCOUNT_ID)
      .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson, is("{\"account_id\":\"" + ACCOUNT_ID.value() + "\"}"));
  }
}