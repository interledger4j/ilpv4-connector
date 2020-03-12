package org.interledger.connector.accounts;

import java.util.List;
import java.util.Optional;

/**
 * Manages finding/creating/deleting {@link AccessToken}s
 */
public interface AccessTokenManager {

  List<AccessToken> findTokensByAccountId(AccountId accountId);

  Optional<AccessToken> findByAccountIdAndRawToken(AccountId accountId, String rawToken);

  void deleteByAccountIdAndId(AccountId accountId, long id);

  void deleteByAccountId(AccountId accountId);

  AccessToken createToken(AccountId accountId);

}
