package org.interledger.connector.accounts.event;

import org.interledger.connector.accounts.AccountId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAccountCreatedEvent.class)
@JsonDeserialize(as = ImmutableAccountCreatedEvent.class)
public interface AccountCreatedEvent {

  AccountId accountId();

  static ImmutableAccountCreatedEvent.Builder builder() {
    return ImmutableAccountCreatedEvent.builder();
  }

}
