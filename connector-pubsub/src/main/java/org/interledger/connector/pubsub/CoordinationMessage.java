package org.interledger.connector.pubsub;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.UUID;

/**
 * Wrapper class for messages that are transmitted via the shared topic
 */
@Value.Immutable
@JsonSerialize(as = ImmutableCoordinationMessage.class)
@JsonDeserialize(as = ImmutableCoordinationMessage.class)
public interface CoordinationMessage {

  /**
   * Used for reflection to determine type used for deserialization without requiring a JsonType annotation on
   * event entities.
   * @return the class name of the original message
   */
  String messageClassName();

  /**
   * The serialized contents of the original message.
   * @return serialized contents of the original message
   */
  byte[] contents();

  /**
   * Metadata to uniquely identify messages received from the topic
   * @return unique uuid for the message
   */
  UUID messageUuid();

  /**
   * Metadata to identify the connector that created the message
   * @return uuid of the container that the message originated from
   */
  UUID applicationCoordinationUuid();

  static ImmutableCoordinationMessage.Builder builder() {
    return ImmutableCoordinationMessage.builder();
  }

}
