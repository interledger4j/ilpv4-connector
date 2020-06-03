package org.interledger.connector.xumm.model.callback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@JsonSerialize(as = ImmutableUserToken.class)
@JsonDeserialize(as = ImmutableUserToken.class)
public interface UserToken {

  static ImmutableUserToken.Builder builder() {
    return ImmutableUserToken.builder();
  }

  @JsonProperty("user_token")
  String userToken();

  @JsonProperty("token_issued")
  Instant tokenIssued();

  @JsonProperty("token_expiration")
  Instant tokenExpiration();

}
