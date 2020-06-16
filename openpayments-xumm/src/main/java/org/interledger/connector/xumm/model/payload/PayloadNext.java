package org.interledger.connector.xumm.model.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.Optional;

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
  HttpUrl always();

  /**
   *
   * @return
   */
  @JsonProperty("no_push_msg_received")
  Optional<String> noPushMessageReceived();


}
