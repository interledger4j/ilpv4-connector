package org.interledger.connector.server.spring.controllers.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Request to create an access token
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableCreateAccessTokenRequest.class)
@JsonSerialize(as = ImmutableCreateAccessTokenRequest.class)
public interface CreateAccessTokenRequest {

  static CreateAccessTokenRequest generatedToken()  {
    return ImmutableCreateAccessTokenRequest.builder().build();
  }

  static CreateAccessTokenRequest forToken(String token)  {
    return ImmutableCreateAccessTokenRequest.builder().token(token).build();
  }


  /**
   * raw token (optionally) specified by the client. If not set, a random token will be generated
   * @return
   */
  Optional<String> token();

}
