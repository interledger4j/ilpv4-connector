package org.interledger.connector.caching;

import org.interledger.connector.fxrates.FxRateOverride;
import org.interledger.connector.persistence.repositories.FxRateOverridesRepository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.TimeUnit;

public class FxRateOverridesLoadingCache {

  private final FxRateOverridesRepository repository;

  private final Cache<String, FxRateOverride> cache;

  /**
   * What is the correct approach to caching here?
   *
   * Options:
   * 1) Assuming the Rust approach with the get/save all methods, use a singleton cache where we convert the entire
   * result set of overrides into a single map. This requires a write-ahead cache where we recompute the cache on
   * save, but also requires some reasonable expiry to account for multiple connectors sharing one database. The expiry
   * would require a `loadAll` approach, though for performance reasons this should be done asynchronously to avoid
   * excessive blocking. It's unclear if Caffeine supports these approaches (but they probably do).
   *
   * This approach involves more memory overhead due to caching everything eagerly, but will offer the best throughput
   * performance due to minimizing blocking.
   *
   * 2) Assuming an individual save approach (non-Rust), use a regular loading cache, though we would need to see
   * if the cache allows for configurable expiration of hits vs misses. This trades lowered memory usage for worse
   * overall throughput since it would result in less predictable availability of data due to not caching against
   * keys that haven't been previously looked up. It does improve write-ahead performance since individual entries
   * can be expired instead of reloading the set due to not being able to easily tell what records changed.
   *
   * Discussion notes:
   * After talking about this more, it seems like option 1 is the best bet. One takeaway from the discussion is,
   * barring a quite frankly ridiculous number of rate overrides, you're likely to use _more_ memory with option 2
   * since you have to cache misses for some period of time (likely as an `Optional`) and misses will in most cases
   * outnumber hits. Option 1 uses zero memory for misses which in almost every situation is the preferable approach.
   *
   *
   * @param repository
   */


  @VisibleForTesting
  public FxRateOverridesLoadingCache(final FxRateOverridesRepository repository) {
    this.repository = repository;
    this.cache = Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES) // Set very high just for testing...
        .maximumSize(5000)
        // The value stored in the Cache is the AccountSettings converted from the entity so we don't have to convert
        // on every ILPv4 packet switch.
        .build();


  }

  public FxRateOverride getOverride(String key) {
    return null;
  }

}
