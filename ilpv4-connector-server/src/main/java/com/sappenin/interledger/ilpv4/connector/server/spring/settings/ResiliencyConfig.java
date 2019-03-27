package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.interledger.core.InterledgerProtocolException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResiliencyConfig {

  @Bean
  CircuitBreakerConfig circuitBreakerConfig() {
    return CircuitBreakerConfig.custom()
      // the failure rate threshold in percentage above which the CircuitBreaker should trip open and start short-circuiting calls
      //.failureRateThreshold(DEFAULT_MAX_FAILURE_THRESHOLD)
      //the wait duration which specifies how long the CircuitBreaker should stay open, before it switches to half open
      //.waitDurationInOpenState(Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE))
      // the size of the ring buffer when the CircuitBreaker is half open
      //.ringBufferSizeInHalfOpenState(DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE)
      // the size of the ring buffer when the CircuitBreaker is closed
      //.ringBufferSizeInClosedState(DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE)
      // a custom Predicate which evaluates if an exception should be recorded as a failure and thus increase the failure rate
      // All InterledgerProtocolExceptions are considered to _not_ be a failure for purpose of circuit breaking.
      // Instead, We want all Reject packets to be propagated back to the initiator so we don't accidentally get a
      // DOS attack from an upstream actor sending something like T03 rejections to itself through us.
      .ignoreExceptions(InterledgerProtocolException.class)
      .enableAutomaticTransitionFromOpenToHalfOpen()
      .build();
  }


}
