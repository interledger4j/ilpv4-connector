package org.interledger.connector.pubsub;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.UUID;

class CoordinationDedupeCache {

    private Cache<UUID, Object> receivedCache;

    private static final Object VALUE = new Object();

    public CoordinationDedupeCache() {
      receivedCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5)) // TODO make configurable
        .build();
    }

    public void record(UUID uuid) {
      receivedCache.put(uuid, VALUE);
    }

    public boolean duplicate(UUID uuid) {
      return receivedCache.getIfPresent(uuid) != null;
    }
}
