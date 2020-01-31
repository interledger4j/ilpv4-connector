package org.interledger.connector.accounts.event;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.pubsub.AbstractCoordinatedEvent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface AccountCreatedEvent {

  AccountId accountId();

  static ImmutableAccountCreatedEvent.Builder builder() {
    return ImmutableAccountCreatedEvent.builder();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableAccountCreatedEvent.class)
  @JsonDeserialize(as = ImmutableAccountCreatedEvent.class)
  abstract class AbstractAccountCreatedEvent extends AbstractCoordinatedEvent implements AccountCreatedEvent {}
}
