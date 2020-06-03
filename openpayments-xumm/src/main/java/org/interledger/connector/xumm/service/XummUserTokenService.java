package org.interledger.connector.xumm.service;

import org.interledger.connector.xumm.model.callback.UserToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class XummUserTokenService {

  private final Map<String, UserToken> userTokensById = new HashMap<>();

  public Optional<UserToken> findByUserId(String accountId) {
    return Optional.ofNullable(userTokensById.get(accountId))
      .filter(token -> token.tokenExpiration().isAfter(Instant.now()));
  }

  public void saveUserToken(String accountId, UserToken userToken) {
    userTokensById.put(accountId, userToken);
  }


}
