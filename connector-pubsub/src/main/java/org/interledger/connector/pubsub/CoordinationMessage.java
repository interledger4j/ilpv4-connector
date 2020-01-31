package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableCoordinationMessage.class)
@JsonDeserialize(as = ImmutableCoordinationMessage.class)
public interface CoordinationMessage {

  String messageClassName();

  byte[] contents();

  UUID messageUuid();

  UUID applicationCoordinationUuid();

  static ImmutableCoordinationMessage.Builder builder() {
    return ImmutableCoordinationMessage.builder();
  }

}
