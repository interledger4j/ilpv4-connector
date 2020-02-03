package org.interledger.connector.accounts;

import java.util.Optional;

/**
 * A loading cache for instances of {@link AccountSettings} that uses some sort of data store as the underlying backing
 * store.
 */
public interface AccountSettingsCache {

  /**
   * Retrieve the account identified by {@code accountId} from the cache, loading it from the repository if not present
   * in the cache.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return An optionally present {@link AccountSettings}. If no account exists in the underlying datastore, then this
   *   methods returns {@link Optional#empty()} .
   */
  Optional<AccountSettings> getAccount(AccountId accountId);

  /**
   * Get an AccountSettings from the cache, or load it from the Repository if it's not cached.
   *
   * @param accountId The {@link AccountId} to load.
   *
   * @return An {@link AccountSettings} from the cache.
   */
  AccountSettings safeGetAccountId(AccountId accountId);

}
