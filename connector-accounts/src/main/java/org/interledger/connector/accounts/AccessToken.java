package org.interledger.connector.accounts;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;

@Value.Immutable
@JsonDeserialize(as = ImmutableAccessToken.class)
@JsonSerialize(as = ImmutableAccessToken.class)
public interface AccessToken {

  static ImmutableAccessToken.Builder builder() {
    return ImmutableAccessToken.builder();
  }

  @Nullable
  Long id();

  AccountId accountId();

  String encryptedToken();

  /**
   * Raw token (not encrypted). Only set on newly minted access tokens.
   * @return
   */
  Optional<String> rawToken();

  Instant createdAt();

}
