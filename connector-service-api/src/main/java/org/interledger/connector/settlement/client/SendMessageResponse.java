package org.interledger.connector.settlement.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.connector.settlement.client.ImmutableSendMessageResponse;

/**
 * An object for modeling a response from the `/messages` endpoint of a Settlement Engine.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSendMessageResponse.class)
@JsonDeserialize(as = ImmutableSendMessageResponse.class)
public interface SendMessageResponse {

  static ImmutableSendMessageResponse.Builder builder() {
    return ImmutableSendMessageResponse.builder();
  }

  /**
   * An opaque response from the counterparty's settlement engine.
   *
   * @return
   */
  byte[] data();
}
