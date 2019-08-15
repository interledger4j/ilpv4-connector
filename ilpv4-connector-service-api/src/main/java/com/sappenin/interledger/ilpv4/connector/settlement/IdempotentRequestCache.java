package com.sappenin.interledger.ilpv4.connector.settlement;

import java.util.Optional;
import java.util.UUID;

/**
 * A cache that can be used to support idempotent requests.
 *
 * @deprecated Will be removed in-favor of Spring @Cachable on the HTTP Controllers.
 */
@Deprecated
public interface IdempotentRequestCache {

  /**
   * Reserve the supplied {@code requestId} so that no other requests can utilize it.
   *
   * @param requestId
   *
   * @return {@code true} if the record was created; {@code false} if the record already existed.
   */
  boolean reserveRequestId(UUID requestId);

  /**
   * Create an idempotency record containing the supplied {@code idempotenyData}.
   *
   * @param httpResponseInfo
   *
   * @return {@code true} if the record was created; {@code false} if the record already existed.
   */
  boolean updateHttpResponseInfo(HttpResponseInfo httpResponseInfo);

  /**
   * Accessor for an optionally present {@link HttpResponseInfo}.
   *
   * @param requestId A {@link UUID} that uniquely identifies the HTTP request.
   *
   * @return A {@link HttpResponseInfo} that correlates to the {@code requestId}, if such an item exists.
   */
  Optional<HttpResponseInfo> getHttpResponseInfo(UUID requestId);
}
