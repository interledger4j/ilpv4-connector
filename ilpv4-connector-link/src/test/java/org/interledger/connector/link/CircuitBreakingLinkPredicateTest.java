package org.interledger.connector.link;

import com.google.common.collect.ImmutableList;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.core.InterledgerErrorCode.F00_BAD_REQUEST;
import static org.interledger.core.InterledgerErrorCode.F01_INVALID_PACKET;
import static org.interledger.core.InterledgerErrorCode.F02_UNREACHABLE;
import static org.interledger.core.InterledgerErrorCode.F03_INVALID_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.F05_WRONG_CONDITION;
import static org.interledger.core.InterledgerErrorCode.F06_UNEXPECTED_PAYMENT;
import static org.interledger.core.InterledgerErrorCode.F07_CANNOT_RECEIVE;
import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE;
import static org.interledger.core.InterledgerErrorCode.F99_APPLICATION_ERROR;
import static org.interledger.core.InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT;
import static org.interledger.core.InterledgerErrorCode.R99_APPLICATION_ERROR;
import static org.interledger.core.InterledgerErrorCode.T00_INTERNAL_ERROR;
import static org.interledger.core.InterledgerErrorCode.T01_PEER_UNREACHABLE;
import static org.interledger.core.InterledgerErrorCode.T02_PEER_BUSY;
import static org.interledger.core.InterledgerErrorCode.T03_CONNECTOR_BUSY;
import static org.interledger.core.InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY;
import static org.interledger.core.InterledgerErrorCode.T05_RATE_LIMITED;
import static org.interledger.core.InterledgerErrorCode.T99_APPLICATION_ERROR;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests {@link CircuitBreakingLink}.
 */
@RunWith(Parameterized.class)
public class CircuitBreakingLinkPredicateTest {

  @Mock
  private Link<?> linkDelegate;

  private CircuitBreakerConfig circuitBreakerConfig;
  private InterledgerErrorCode errorCode;

  private CircuitBreakingLink circuitBreakingLink;
  private CircuitBreaker.State expectedCircuitBreakerState;

  public CircuitBreakingLinkPredicateTest(
    final InterledgerErrorCode errorCode, final CircuitBreaker.State expectedCircuitBreakerState
  ) {
    this.errorCode = Objects.requireNonNull(errorCode);
    this.expectedCircuitBreakerState = Objects.requireNonNull(expectedCircuitBreakerState);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // T Family
      new Object[]{T00_INTERNAL_ERROR, CircuitBreaker.State.CLOSED},
      new Object[]{T01_PEER_UNREACHABLE, CircuitBreaker.State.CLOSED},
      new Object[]{T02_PEER_BUSY, CircuitBreaker.State.CLOSED},
      new Object[]{T03_CONNECTOR_BUSY, CircuitBreaker.State.CLOSED},
      new Object[]{T04_INSUFFICIENT_LIQUIDITY, CircuitBreaker.State.CLOSED},
      new Object[]{T05_RATE_LIMITED, CircuitBreaker.State.CLOSED},
      new Object[]{T99_APPLICATION_ERROR, CircuitBreaker.State.CLOSED},

      // R Family
      new Object[]{R01_INSUFFICIENT_SOURCE_AMOUNT, CircuitBreaker.State.CLOSED},
      new Object[]{R02_INSUFFICIENT_TIMEOUT, CircuitBreaker.State.CLOSED},
      new Object[]{R99_APPLICATION_ERROR, CircuitBreaker.State.CLOSED},

      // F Family
      new Object[]{F00_BAD_REQUEST, CircuitBreaker.State.CLOSED},
      new Object[]{F01_INVALID_PACKET, CircuitBreaker.State.CLOSED},
      new Object[]{F02_UNREACHABLE, CircuitBreaker.State.CLOSED},
      new Object[]{F03_INVALID_AMOUNT, CircuitBreaker.State.CLOSED},
      new Object[]{F04_INSUFFICIENT_DST_AMOUNT, CircuitBreaker.State.CLOSED},
      new Object[]{F05_WRONG_CONDITION, CircuitBreaker.State.CLOSED},
      new Object[]{F06_UNEXPECTED_PAYMENT, CircuitBreaker.State.CLOSED},
      new Object[]{F07_CANNOT_RECEIVE, CircuitBreaker.State.CLOSED},
      new Object[]{F08_AMOUNT_TOO_LARGE, CircuitBreaker.State.CLOSED},
      new Object[]{F99_APPLICATION_ERROR, CircuitBreaker.State.CLOSED}
    );
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.circuitBreakerConfig = CircuitBreakerConfig.custom()
      .ignoreExceptions(InterledgerProtocolException.class)
      .enableAutomaticTransitionFromOpenToHalfOpen()
      .build();

    when(linkDelegate.getLinkId()).thenReturn(LinkId.of("foo"));

    this.circuitBreakingLink = new CircuitBreakingLink(linkDelegate, circuitBreakerConfig);
  }

  /**
   * Verify that if an InterledgerProtocolException is ever thrown, it will be ignored by the circuit breaker.
   */
  @Test
  public void validateIgnoredExceptions() throws InterruptedException {
    final InterledgerProtocolException exception =
      new InterledgerProtocolException(InterledgerRejectPacket.builder()
        .triggeredBy(InterledgerAddress.of("test.foo"))
        .code(errorCode)
        .build());

    when(linkDelegate.sendPacket(any())).thenThrow(exception);

    final CountDownLatch doneSignal = new CountDownLatch(1);
    circuitBreakingLink.getCircuitBreaker().getEventPublisher().onIgnoredError(error -> {
      assertThat(error.getThrowable() instanceof InterledgerProtocolException, is(true));
      doneSignal.countDown();
    });

    circuitBreakingLink.getCircuitBreaker().getEventPublisher().onError(error -> {
      fail("Should not throw normal exception!");
    });


    try {
      circuitBreakingLink.sendPacket(mock(InterledgerPreparePacket.class));
      fail("Should have thrown an InterledgerProtocolException");
    } catch (InterledgerProtocolException e) {
      assertThat(
        circuitBreakingLink.getCircuitBreaker().getState().equals(expectedCircuitBreakerState),
        is(true)
      );
    }

    if (!doneSignal.await(10, TimeUnit.MILLISECONDS)) {
      fail("Countdown latch not triggered!");
    }
  }

  @Test
  public void validateUnIgnoredExceptions() throws InterruptedException {
    when(linkDelegate.sendPacket(any())).thenThrow(new RuntimeException("hello"));

    final CountDownLatch doneSignal = new CountDownLatch(1);
    circuitBreakingLink.getCircuitBreaker().getEventPublisher().onIgnoredError(error -> {
      fail("Should not throw ignored exception!");
    });

    circuitBreakingLink.getCircuitBreaker().getEventPublisher().onError(error -> {
      assertThat(error.getThrowable().getMessage(), is("hello"));
      assertThat(error.getThrowable() instanceof RuntimeException, is(true));
      doneSignal.countDown();
    });

    try {
      circuitBreakingLink.sendPacket(mock(InterledgerPreparePacket.class));
      fail("Should have thrown an InterledgerProtocolException");
    } catch (RuntimeException e) {
      assertThat(
        circuitBreakingLink.getCircuitBreaker().getState().equals(expectedCircuitBreakerState),
        is(true)
      );
    }

    if (!doneSignal.await(10, TimeUnit.MILLISECONDS)) {
      fail("Countdown latch not triggered!");
    }
  }

}
