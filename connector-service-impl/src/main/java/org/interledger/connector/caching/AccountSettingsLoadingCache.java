package org.interledger.connector.caching;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.AccountSettingsCache;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * A loading cache for instances of {@link AccountSettings} that uses a {@link AccountSettingsRepository} to source its
 * data.
 */
public class AccountSettingsLoadingCache implements AccountSettingsCache {

  /**
   * A function that accepts inputs required to load Account settings from a data store and return them as an instance
   * of {@link AccountSettings}. This function is used so the AccountEntity doesn't have to be converted on every cache
   * hit.
   */
  private static final BiFunction<AccountId, AccountSettingsRepository, Optional<AccountSettings>>
    ACCOUNT_SETTINGS_CACHE_POPULATOR =
    (accountId, accountSettingsRepository) -> accountSettingsRepository.findByAccountIdWithConversion(accountId);

  private final AccountSettingsRepository accountSettingsRepository;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache.
  private final Cache<AccountId, Optional<AccountSettings>> accountSettingsCache;

  /**
   * For testing purposes.
   */
  @VisibleForTesting
  public AccountSettingsLoadingCache(final AccountSettingsRepository accountSettingsRepository) {
    this(accountSettingsRepository, Caffeine.newBuilder()
      // No stats recording in the cache because this is only used for testing...
      .expireAfterWrite(15, TimeUnit.MINUTES) // Set very high just for testing...
      .maximumSize(5000)
      // The value stored in the Cache is the AccountSettings converted from the entity so we don't have to convert
      // on every ILPv4 packet switch.
      .build((accountId) -> accountSettingsRepository.findByAccountIdWithConversion(accountId))
    );
  }

  public AccountSettingsLoadingCache(
    final AccountSettingsRepository accountSettingsRepository,
    final Cache<AccountId, Optional<AccountSettings>> accountSettingsCache
  ) {
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.accountSettingsCache = Objects.requireNonNull(accountSettingsCache);
  }

  /**
   * Retrieve the account identified by {@code accountId} from the cache, loading it from the repository if not present
   * in the cache.
   *
   * @param accountId The {@link AccountId} of the account to retrieve.
   *
   * @return An optionally present {@link AccountSettings}. If no account exists in the underlying datastore, then this
   *   methods returns {@link Optional#empty()} .
   */
  public Optional<AccountSettings> getAccount(final AccountId accountId) {
    return this.accountSettingsCache.get(
      accountId,
      // Populate the cache if necessary...
      $ -> ACCOUNT_SETTINGS_CACHE_POPULATOR.apply(accountId, accountSettingsRepository)
    );
  }

  /**
   * Get an AccountSettings from the cache, or load it from the {@link AccountSettingsRepository} if it's not cached.
   *
   * @param accountId The {@link AccountId} to lookup.
   *
   * @return An {@link AccountSettings} from the cache.
   */
  public AccountSettings safeGetAccountId(final AccountId accountId) {
    return this.getAccount(accountId).orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }
}
