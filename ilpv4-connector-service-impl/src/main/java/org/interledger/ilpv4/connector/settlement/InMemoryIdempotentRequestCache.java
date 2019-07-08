package org.interledger.ilpv4.connector.settlement;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.settlement.HttpResponseInfo;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentRequestCache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An in-memory implementation of {@link IdempotentRequestCache}. This implementation does not support an HA
 * environment, and should not be used in production scenarios operating more than a single node.
 */
public class InMemoryIdempotentRequestCache implements IdempotentRequestCache {

  private final Map<UUID, Optional<HttpResponseInfo>> responses;

  public InMemoryIdempotentRequestCache() {
    this.responses = Maps.newConcurrentMap();
  }

  @Override
  public boolean reserveRequestId(final UUID requestId) {
    Objects.requireNonNull(requestId);
    return responses.putIfAbsent(requestId, Optional.empty()) == null;
  }

  @Override
  public boolean updateHttpResponseInfo(final HttpResponseInfo httpResponseInfo) {
    Objects.requireNonNull(httpResponseInfo);

    // We expect a record to exist if we're updating it.
    return responses.put(httpResponseInfo.requestId(), Optional.of(httpResponseInfo)) != null;
  }

  @Override
  public Optional<HttpResponseInfo> getHttpResponseInfo(final UUID requestId) {
    Objects.requireNonNull(requestId);
    return responses.get(requestId);
  }
}
