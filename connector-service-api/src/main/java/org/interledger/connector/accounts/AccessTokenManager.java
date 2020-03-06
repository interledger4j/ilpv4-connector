package org.interledger.connector.accounts;

import java.util.List;
import java.util.Optional;

public interface AccessTokenManager {

  List<AccessToken> findTokensByAccountId(AccountId accountId);

  Optional<AccessToken> findByAccountIdAndRawToken(AccountId accountId, String rawToken);

  Optional<AccessToken> findByAccountIdAndId(AccountId accountId, long id);

  void deleteByAccountIdAndId(AccountId accountId, long id);

  void deleteByAccountId(AccountId accountId);

  AccessToken createToken(AccountId accountId, String token);

  AccessToken createToken(AccountId accountId);

}
