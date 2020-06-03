package org.interledger.connector.xumm.model.callback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMeta.class)
@JsonDeserialize(as = ImmutableMeta.class)
public interface Meta {

  static ImmutableMeta.Builder builder() {
    return ImmutableMeta.builder();
  }

  String url();

  @JsonProperty("application_uuidv4")
  String applicationUuidV4();

  @JsonProperty("payload_uuidv4")
  String payloadUuidV4();

}
