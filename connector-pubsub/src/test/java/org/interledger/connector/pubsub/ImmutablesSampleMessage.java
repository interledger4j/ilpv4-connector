package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableImmutablesSampleMessage.class)
@JsonDeserialize(as = ImmutableImmutablesSampleMessage.class)
public interface ImmutablesSampleMessage {

  default String value() {
    return "immutable message";
  }

  static ImmutableImmutablesSampleMessage.Builder builder() {
    return ImmutableImmutablesSampleMessage.builder();
  }
}
