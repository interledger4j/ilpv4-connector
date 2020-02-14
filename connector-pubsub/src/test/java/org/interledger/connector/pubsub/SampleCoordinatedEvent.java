package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface SampleCoordinatedEvent {

  default String value() {
    return "immutable message";
  }

  static ImmutableSampleCoordinatedEvent.Builder builder() {
    return ImmutableSampleCoordinatedEvent.builder();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableSampleCoordinatedEvent.class)
  @JsonDeserialize(as = ImmutableSampleCoordinatedEvent.class)
  abstract class AbstractSampleCoordinatedEvent extends AbstractCoordinatedEvent implements SampleCoordinatedEvent {}

}
