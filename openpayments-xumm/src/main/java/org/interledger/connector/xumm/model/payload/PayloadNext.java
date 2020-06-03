package org.interledger.connector.xumm.model.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePayloadNext.class)
@JsonDeserialize(as = ImmutablePayloadNext.class)
public interface PayloadNext {

  static ImmutablePayloadNext.Builder builder() {
    return ImmutablePayloadNext.builder();
  }

  /**
   * URL that user can be redirected to
   * @return
   */
  String always();

  /**
   *
   * @return
   */
  @JsonProperty("no_push_msg_received")
  String noPushMessageReceived();


}
