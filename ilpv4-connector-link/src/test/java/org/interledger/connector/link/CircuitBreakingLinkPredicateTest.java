package org.interledger.connector.link;

import com.google.common.collect.ImmutableList;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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

/**
 * Unit tests {@link CircuitBreakingLink}.
 */
@RunWith(Parameterized.class)
public class CircuitBreakingLinkPredicateTest {

  private InterledgerErrorCode errorCode;

  private boolean expectedPredicatedResult;

  public CircuitBreakingLinkPredicateTest(
    final InterledgerErrorCode errorCode, final boolean expectedPredicatedResult
  ) {
    this.errorCode = Objects.requireNonNull(errorCode);
    this.expectedPredicatedResult = expectedPredicatedResult;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> errorCodes() {
    return ImmutableList.of(
      // T Family
      new Object[]{T00_INTERNAL_ERROR, true},
      new Object[]{T01_PEER_UNREACHABLE, true},
      new Object[]{T02_PEER_BUSY, true},
      new Object[]{T03_CONNECTOR_BUSY, true},
      new Object[]{T04_INSUFFICIENT_LIQUIDITY, true},
      new Object[]{T05_RATE_LIMITED, true},
      new Object[]{T99_APPLICATION_ERROR, true},

      // R Family
      new Object[]{R01_INSUFFICIENT_SOURCE_AMOUNT, true},
      new Object[]{R02_INSUFFICIENT_TIMEOUT, true},
      new Object[]{R99_APPLICATION_ERROR, true},

      // F Family
      new Object[]{F00_BAD_REQUEST, true},
      new Object[]{F01_INVALID_PACKET, true},
      new Object[]{F02_UNREACHABLE, true},
      new Object[]{F03_INVALID_AMOUNT, true},
      new Object[]{F04_INSUFFICIENT_DST_AMOUNT, true},
      new Object[]{F05_WRONG_CONDITION, true},
      new Object[]{F06_UNEXPECTED_PAYMENT, true},
      new Object[]{F07_CANNOT_RECEIVE, true},
      new Object[]{F08_AMOUNT_TOO_LARGE, true},
      new Object[]{F99_APPLICATION_ERROR, true}
    );
  }

  @Test
  public void validatePredicate() {
    final InterledgerProtocolException exception =
      new InterledgerProtocolException(InterledgerRejectPacket.builder()
        .triggeredBy(InterledgerAddress.of("test.foo"))
        .code(errorCode)
        .build());

    assertThat("Unexpected predicate result for " + this.errorCode,
      CircuitBreakerConfig.DEFAULT_RECORD_FAILURE_PREDICATE.test(exception), is(this.expectedPredicatedResult)
    );
  }

}