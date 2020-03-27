package org.interledger.connector.jackson.modules;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.Before;

/**
 * Abstract class that holds common functionality for testing Id (de)serialization using Jackson.
 */
public abstract class AbstractIdTest {

  protected ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = ObjectMapperFactory.create();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableAccountIdContainer.class)
  @JsonDeserialize(as = ImmutableAccountIdContainer.class)
  public interface AccountIdContainer {

    @JsonProperty("account_id")
    AccountId getAccountId();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableSettlementAccountIdContainer.class)
  @JsonDeserialize(as = ImmutableSettlementAccountIdContainer.class)
  public interface SettlementAccountIdContainer {

    @JsonProperty("settlement_account_id")
    SettlementEngineAccountId getSettlementAccountId();
  }
}
