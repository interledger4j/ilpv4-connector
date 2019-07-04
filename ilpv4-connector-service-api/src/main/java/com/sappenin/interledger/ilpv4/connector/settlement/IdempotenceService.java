package com.sappenin.interledger.ilpv4.connector.settlement;

import java.util.Optional;
import java.util.UUID;

/**
 * A service that can be used to enforce idempotent requests.
 */
public interface IdempotenceService {

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
   * @param idempotentResponseInfo
   *
   * @return {@code true} if the record was created; {@code false} if the record already existed.
   */
  boolean updateIdempotenceRecord(IdempotentResponseInfo idempotentResponseInfo);

  Optional<IdempotentResponseInfo> getIdempotenceRecord(UUID requestId);
}
