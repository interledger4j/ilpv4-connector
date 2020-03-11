package org.interledger.connector.accounts.event;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.pubsub.AbstractCoordinatedEvent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface AccountCredentialsUpdatedEvent {

  AccountId accountId();

  static ImmutableAccountCredentialsUpdatedEvent.Builder builder() {
    return ImmutableAccountCredentialsUpdatedEvent.builder();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableAccountCredentialsUpdatedEvent.class)
  @JsonDeserialize(as = ImmutableAccountCredentialsUpdatedEvent.class)
  abstract class AbstractAccountCredentialsUpdatedEvent extends AbstractCoordinatedEvent implements AccountCredentialsUpdatedEvent {}
}
