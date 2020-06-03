package org.interledger.connector.xumm.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableCustomMeta.class)
@JsonDeserialize(as = ImmutableCustomMeta.class)
public interface CustomMeta {

  static ImmutableCustomMeta.Builder builder() {
    return ImmutableCustomMeta.builder();
  }

  /**
   * Your own identifier for this payload. This identifier must be unique.
   * If duplicate, an error code 409 will be returned (max 40 positions)
   * @return
   */
  String identifier();

  /**
   * A custom JSON object containing metadata, attached to this specific payload (stringified max 1500 positions)
   * @return
   */
  Optional<Object> blob();

  /**
   * A message (instruction, reason for signing) to display to the XUMM (signing) user (max 280 positions)
   * @return
   */
  String instruction();

}
