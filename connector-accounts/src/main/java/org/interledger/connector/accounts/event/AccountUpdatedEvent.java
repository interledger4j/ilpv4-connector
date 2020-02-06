package org.interledger.connector.accounts.event;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.pubsub.AbstractCoordinatedEvent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface AccountUpdatedEvent {

  AccountId accountId();

  static ImmutableAccountUpdatedEvent.Builder builder() {
    return ImmutableAccountUpdatedEvent.builder();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableAccountUpdatedEvent.class)
  @JsonDeserialize(as = ImmutableAccountUpdatedEvent.class)
  abstract class AbstractAccountUpdatedEvent extends AbstractCoordinatedEvent implements AccountUpdatedEvent {}
}
