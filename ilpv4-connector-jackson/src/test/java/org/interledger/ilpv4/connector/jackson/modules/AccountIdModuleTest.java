package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link AccountIdModule}.
 */
public class AccountIdModuleTest extends AbstractIdTest {

  private static final AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID().toString());
  protected ObjectMapper objectMapperWithoutModule;

  @Before
  public void setUp() {
    objectMapperWithoutModule = ObjectMapperFactory.create();
    objectMapper.registerModule(new AccountIdModule());
  }

  @Test
  public void shouldSerializeAndDeserialize() throws IOException {
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(ACCOUNT_ID)
      .build();

    final String json = objectMapper.writeValueAsString(expectedContainer);
    final AccountIdContainer actualContainer = objectMapper
      .readValue(json, AccountIdContainer.class);

    assertThat(actualContainer, is(expectedContainer));
  }

  @Test
  public void shouldNotSerializeAndDeserialize() throws IOException {
    final AccountIdContainer expectedContainer = ImmutableAccountIdContainer.builder()
      .accountId(ACCOUNT_ID)
      .build();

    final String actualJson = objectMapperWithoutModule.writeValueAsString(expectedContainer);
    final AccountIdContainer decodedJson = objectMapperWithoutModule.readValue(actualJson, AccountIdContainer.class);
    assertThat(decodedJson, is(expectedContainer));
  }
}