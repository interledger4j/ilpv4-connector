package org.interledger.connector.server.spring.controllers.settlement;

import com.google.common.base.Preconditions;
import org.interledger.connector.settlement.SettlementConstants;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * A {@link KeyGenerator} that inspects a Contoller method and looks for the {@link SettlementConstants#IDEMPOTENCY_KEY}
 * header in order to cache around it. This facility allows the Spring controllers to cache using spring-data-cache as
 * opposed to having to manually store request/response in Redis or some other cache.
 */
public class SettlementEngineIdempotencyKeyGenerator implements KeyGenerator {
  /**
   * Generate a key for the given method and its parameters.
   *
   * @param target the target instance
   * @param method the method being called
   * @param params the method parameters (with any var-args expanded)
   *
   * @return a generated key
   */
  @Override
  public Object generate(Object target, Method method, Object... params) {
    if (target != null && SettlementController.class.isAssignableFrom(target.getClass())) {
      Preconditions.checkArgument(params.length > 0, "params is expected to have at least 1 value");
      // This KeyGenerator will be called whenever a method in {@link SettlementController} is called, and the first
      // value is always idempotency-key.
      Object idempotencyKey = params[0] != null ? params[0] : null;
      return idempotencyKey;
    } else {
      return null;
    }
  }
}
