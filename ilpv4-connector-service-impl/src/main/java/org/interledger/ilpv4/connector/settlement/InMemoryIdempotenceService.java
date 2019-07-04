package org.interledger.ilpv4.connector.settlement;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotenceService;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentResponseInfo;
import org.interledger.ilpv4.connector.core.settlement.Quantity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An in-memory implementation of {@link IdempotenceService}. This implementation does not support an HA environment,
 * and should not be used in production scenarios operating more than a single node.
 */
public class InMemoryIdempotenceService implements IdempotenceService {

  private static final Quantity ZERO_QUANTITY = Quantity.builder().scale(0).amount(BigInteger.ZERO).build();

  private static final IdempotentResponseInfo PLACEHOLDER = IdempotentResponseInfo.builder()
    .requestId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    .responseBody(ZERO_QUANTITY)
    .responseStatus(HttpStatus.BAD_REQUEST)
    .responseHeaders(new HttpHeaders())
    .build();

  private final Map<UUID, IdempotentResponseInfo> responses;

  public InMemoryIdempotenceService() {
    this.responses = Maps.newConcurrentMap();
  }

  @Override
  public boolean reserveRequestId(final UUID requestId) {
    Objects.requireNonNull(requestId);
    return responses.putIfAbsent(requestId, PLACEHOLDER) == null;
  }

  @Override
  public boolean updateIdempotenceRecord(final IdempotentResponseInfo idempotentResponseInfo) {
    Objects.requireNonNull(idempotentResponseInfo);

    // We expect a record to exist if we're updating it.
    return responses.putIfAbsent(idempotentResponseInfo.requestId(), idempotentResponseInfo) != null;
  }

  @Override
  public Optional<IdempotentResponseInfo> getIdempotenceRecord(final UUID requestId) {
    Objects.requireNonNull(requestId);
    return Optional.ofNullable(responses.get(requestId));
  }
}
