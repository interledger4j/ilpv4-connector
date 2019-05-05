package org.interledger.ilpv4.connector.jackson.modules;

import org.interledger.connector.accounts.AccountId;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link AccountIdDeserializer}.
 */
public class AccountIdDeserializerTest extends AbstractIdTest {

  private static final String FOO = "FOO";
  private AccountId USER_ID = AccountId.of(UUID.randomUUID());

  @Test
  public void shouldDeserialize() throws IOException {
    final AccountId actual = objectMapper
      .readValue("\"" + USER_ID.value() + "\"", AccountId.class);

    assertThat(actual, is(USER_ID));
  }

  @Test
  public void shouldDeserializeInContainer() throws IOException {
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(USER_ID)
      .build();

    final AccountIdContainer actualContainer = objectMapper.readValue(
      "{\"user_id\":\"" + USER_ID.value() + "\"}",
      AccountIdContainer.class
    );

    assertThat(actualContainer, is(expectedContainer));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotDeserializeInvalidKey() throws IOException {
    try {
      objectMapper.readValue("\"" + FOO + "\"", AccountId.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Invalid UUID string: FOO"));
      throw e;
    }
  }
}