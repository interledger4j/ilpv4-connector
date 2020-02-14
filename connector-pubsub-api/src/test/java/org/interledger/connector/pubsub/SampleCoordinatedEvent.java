package org.interledger.connector.pubsub;

import org.immutables.value.Value;

public interface SampleCoordinatedEvent {

  default String value() {
    return "immutable message";
  }

  static ImmutableSampleCoordinatedEvent.Builder builder() {
    return ImmutableSampleCoordinatedEvent.builder();
  }

  @Value.Immutable
  abstract class AbstractSampleCoordinatedEvent extends AbstractCoordinatedEvent implements SampleCoordinatedEvent {}

}
