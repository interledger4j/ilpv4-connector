package org.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.settlement.IdempotenceService;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentResponseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link IdempotenceService} that uses Redis as a backing store.
 */
public class RedisIdempotenceService implements IdempotenceService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final RedisTemplate<UUID, IdempotentResponseInfo> redisTemplate;

  public RedisIdempotenceService(final RedisTemplate<UUID, IdempotentResponseInfo> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean reserveRequestId(UUID requestId) {
    Objects.requireNonNull(requestId);

    if (redisTemplate.opsForValue().setIfAbsent(requestId, null)) {
      // First time, so set the expiry
      logger.debug("Reserving IdempotenceData in Redis for RequestId `{}`", requestId);
      redisTemplate.expire(requestId, 86400, TimeUnit.SECONDS);
      return true;
    } else {
      logger.debug("IdempotenceData for RequestId `{}` already existed in Redis", requestId);
      return false;
    }
  }

  @Override
  public boolean updateIdempotenceRecord(final IdempotentResponseInfo idempotentResponseInfo) {
    Objects.requireNonNull(idempotentResponseInfo);

    final UUID requestId = idempotentResponseInfo.requestId();
    if (redisTemplate.opsForValue().setIfPresent(requestId, idempotentResponseInfo)) {
      // First time, so set the expiry
      logger.debug("Updating IdempotenceData in Redis for RequestId `{}`: {}", requestId, idempotentResponseInfo);
      return true;
    } else {
      logger.debug("IdempotenceData for RequestId `{}` already existed in Redis", requestId);
      return false;
    }
  }

  @Override
  public Optional<IdempotentResponseInfo> getIdempotenceRecord(UUID requestId) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(requestId));
  }
}
