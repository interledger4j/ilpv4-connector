package org.interledger.connector.link;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractLink;
import org.interledger.link.Link;
import org.interledger.link.LinkHandler;
import org.interledger.link.LinkSettings;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link Link} that wraps an internal Link-delegate and provides Circuit breaking functionality.
 */
public class CircuitBreakingLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  private final Link<?> linkDelegate;

  private final CircuitBreakerRegistry circuitBreakerRegistry;

  /**
   * Required-args constructor.
   *
   * @param linkDelegate The {@link Link} to wrap in a circuit breaker.
   */
  @VisibleForTesting
  CircuitBreakingLink(final Link<?> linkDelegate) {
    this(linkDelegate, CircuitBreakerConfig.custom()
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
        .build()
    );
  }

  /**
   * Required-args constructor.
   *
   * @param circuitBreakerConfig Timeout value in milliseconds for a command.
   * @param linkDelegate         The {@link Link} to wrap in a circuit breaker.
   */
  public CircuitBreakingLink(
      final Link<?> linkDelegate,
      final CircuitBreakerConfig circuitBreakerConfig
  ) {
    super(linkDelegate.getOperatorAddressSupplier(), linkDelegate.getLinkSettings());

    this.linkDelegate = Objects.requireNonNull(linkDelegate);
    this.circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    // TODO: Once a load-test is in place, consider not looking this up for _every_ packet send.
    final CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(this.getLinkId().value());

    // The circuitBreaker will catch any exceptions that aren't InterledgerProtocolException and consider those for
    // breaking purposes.
    return CircuitBreaker.decorateFunction(circuitBreaker, linkDelegate::sendPacket).apply(preparePacket);
  }

  // Overridden just to be sure that some outside thing doesn't register a handler directly.
  @Override
  public void registerLinkHandler(LinkHandler dataHandler) throws LinkHandlerAlreadyRegisteredException {
    this.linkDelegate.registerLinkHandler(dataHandler);
  }

  @Override
  public Optional<LinkHandler> getLinkHandler() {
    return this.linkDelegate.getLinkHandler();
  }

  @Override
  public void unregisterLinkHandler() {
    this.linkDelegate.unregisterLinkHandler();
  }

  public <T> T getLinkDelegateTyped() {
    return (T) this.linkDelegate;
  }

  @VisibleForTesting
  CircuitBreaker getCircuitBreaker() {
    return circuitBreakerRegistry.circuitBreaker(this.getLinkId().value());
  }
}
