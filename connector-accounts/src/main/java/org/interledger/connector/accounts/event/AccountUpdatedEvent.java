package org.interledger.connector.accounts.event;

import org.interledger.connector.accounts.AccountId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAccountUpdatedEvent.class)
@JsonDeserialize(as = ImmutableAccountUpdatedEvent.class)
public interface AccountUpdatedEvent {

  AccountId accountId();

  static ImmutableAccountUpdatedEvent.Builder builder() {
    return ImmutableAccountUpdatedEvent.builder();
  }
}
