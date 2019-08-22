package org.interledger.ilpv4.connector.jackson.modules;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.LinkId;
import org.interledger.connector.link.LinkType;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
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
  @JsonSerialize(as = ImmutableLinkIdContainer.class)
  @JsonDeserialize(as = ImmutableLinkIdContainer.class)
  public interface LinkIdContainer {

    @JsonProperty("link_id")
    LinkId getLinkId();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableLinkTypeContainer.class)
  @JsonDeserialize(as = ImmutableLinkTypeContainer.class)
  public interface LinkTypeContainer {

    @JsonProperty("link_type")
    LinkType getLinkType();
  }

}
