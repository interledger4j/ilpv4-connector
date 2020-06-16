package org.interledger.connector.xumm.model.payload;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePayloadRequestResponse.class)
@JsonDeserialize(as = ImmutablePayloadRequestResponse.class)
public interface PayloadRequestResponse {

  static ImmutablePayloadRequestResponse.Builder builder() {
    return ImmutablePayloadRequestResponse.builder();
  }

  String uuid();

  PayloadNext next();

  PayloadRefs refs();

  boolean pushed();

}
