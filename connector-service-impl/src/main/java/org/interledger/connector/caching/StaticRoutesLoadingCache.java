package org.interledger.connector.caching;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.Objects;

public class StaticRoutesLoadingCache {

  private final LoadingCache<String, LoadingCache<InterledgerAddressPrefix, AccountId>> singletonCache;

  private final StaticRoutesRepository staticRoutesRepository;

  private final String SINGLETON_KEY = "key";

  public StaticRoutesLoadingCache(final StaticRoutesRepository staticRoutesRepository) {
    Objects.requireNonNull(staticRoutesRepository);
    this.staticRoutesRepository = staticRoutesRepository;
    this.singletonCache = Caffeine.newBuilder()
        .refreshAfterWrite(Duration.ofSeconds(15)) // TODO make configurable?
        .build(k -> {
          LoadingCache<InterledgerAddressPrefix, AccountId> cache = Caffeine.newBuilder()
              .build(prefix -> staticRoutesRepository.getByPrefix(prefix).accountId());
          this.staticRoutesRepository.getAllStaticRoutes().stream().forEach(r -> cache.put(r.prefix(), r.accountId()));
          return cache;
        });
  }

  public void refreshStaticRoute(StaticRoute staticRoute) {
    singletonCache.get(SINGLETON_KEY).refresh(staticRoute.prefix());
  }

  public void refreshAll() {
    singletonCache.refresh(SINGLETON_KEY);
  }

  public void removeStaticRoute(InterledgerAddressPrefix prefix) {
    singletonCache.get(SINGLETON_KEY).invalidate(prefix);
  }

  public AccountId getNextHopAccountIdForPrefix(InterledgerAddressPrefix prefix) {
    return singletonCache.get(SINGLETON_KEY).getIfPresent(prefix);
  }

}
