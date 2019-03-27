package org.interledger.connector.link;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests {@link CircuitBreakingLink}.
 */
public class CircuitBreakingLinkTest {

  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .destination(InterledgerAddress.of("test.foo"))
    .amount(BigInteger.TEN)
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .expiresAt(Instant.now().plusSeconds(50))
    .build();

  private static final String LINK_ID = "123";
  private static final CircuitBreakerConfig CONFIG = CircuitBreakerConfig.custom()
    .failureRateThreshold(2)
    .ringBufferSizeInClosedState(2)
    .ringBufferSizeInHalfOpenState(2)
    .ignoreExceptions(InterledgerProtocolException.class)
    .enableAutomaticTransitionFromOpenToHalfOpen()
    .build();

  @Mock
  private Link<LinkSettings> linkMock;

  private CircuitBreakingLink link;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    when(linkMock.getLinkId()).thenReturn(Optional.of(LinkId.of(LINK_ID)));
    when(linkMock.sendPacket(PREPARE_PACKET)).thenReturn(reject(InterledgerErrorCode.T03_CONNECTOR_BUSY));

    this.link = new CircuitBreakingLink(linkMock, CONFIG);
  }

  @Test
  public void sendRecordIlpExceptions() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID);

    // Simulate a failure attempt
    circuitBreaker.onError(0,
      new InterledgerProtocolException(reject(InterledgerErrorCode.T02_PEER_BUSY)));
    // CircuitBreaker is still CLOSED, because 1 failure is allowed
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));

    // Simulate a 2nd failure attempt
    circuitBreaker.onError(0, new InterledgerProtocolException(reject(InterledgerErrorCode.T03_CONNECTOR_BUSY)));
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));

    // Simulate a 3rd failure attempt
    circuitBreaker.onError(0, new InterledgerProtocolException(reject(InterledgerErrorCode.T02_PEER_BUSY)));
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));
  }

  @Test
  public void sendRecordNonIlpExceptions() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID);

    // Simulate a failure attempt
    circuitBreaker.onError(0, new RuntimeException("foo"));
    // CircuitBreaker is still CLOSED, because 1 failure is allowed
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));

    // Simulate a 2nd failure attempt
    circuitBreaker.onError(0, new RuntimeException("foo"));
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.OPEN));

    // Simulate a 3rd failure attempt
    circuitBreaker.onError(0, new RuntimeException("foo"));
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.OPEN));
  }


  @Test
  public void sendPacketWithIlpExceptionsBelowThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID);

    // Simulate a failure attempt
    CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
      CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
    Try<InterledgerResponsePacket> result = Try.of(() -> function.apply(PREPARE_PACKET));

    //Then
    // CircuitBreaker is still CLOSED, because 1 Reject has not been recorded as a failure
    assertThat(result.isFailure(), is(false));
    assertThat(result.failed().isEmpty(), is(true));
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));
  }

  @Test
  public void sendPacketWithIlpExceptionsAboveThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID);

    Try<InterledgerResponsePacket> result = null;
    for (int i = 0; i < 2; i++) {
      //When
      CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
        CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
      result = Try.of(() -> function.apply(PREPARE_PACKET));
    }

    //Then
    assertThat(result.isFailure(), is(false));
    assertThat(result.failed().isEmpty(), is(true));
    // CircuitBreaker is still CLOSED, because 1 Reject has been recorded as a failure
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));
  }

  @Test
  public void sendPacketWithNumFailuresBelowThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID);

    doThrow(new RuntimeException("foo")).when(linkMock).sendPacket(any());

    // Simulate a failure attempts
    CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
      CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
    Try<InterledgerResponsePacket> result = Try.of(() -> function.apply(PREPARE_PACKET));

    //Then
    assertThat(result.isFailure(), is(true));
    // CircuitBreaker is still CLOSED, because 1 Reject has not been recorded as a failure
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.CLOSED));
    assertThat(result.failed().get().getClass().toString(), is(RuntimeException.class.toString()));
  }

  @Test
  public void sendPacketWithNumFailuresAboveThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID);
    doThrow(new RuntimeException("foo")).when(linkMock).sendPacket(any());

    Try<InterledgerResponsePacket> result = null;
    for (int i = 0; i < 2; i++) {
      //When
      CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
        CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
      result = Try.of(() -> function.apply(PREPARE_PACKET));
    }

    //Then
    assertThat(result.isFailure(), is(true));
    // CircuitBreaker is still CLOSED, because 1 Reject has been recorded as a failure
    assertThat(circuitBreaker.getState(), is(CircuitBreaker.State.OPEN));
    assertThat(
      "Expected RuntimeException but was: " + result.failed().get().getClass(),
      result.failed().get().getClass().getName(),
      is(RuntimeException.class.getName())
    );
  }

  @Test
  public void getLinkId() {
    this.link.getLinkId();
    verify(linkMock).getLinkId();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void getOperatorAddressSupplier() {
    this.link.getOperatorAddressSupplier();
    verify(linkMock).getOperatorAddressSupplier();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void getLinkSettings() {
    this.link.getLinkSettings();
    verify(linkMock).getLinkSettings();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void registerLinkHandler() {
    this.link.registerLinkHandler(null);
    verify(linkMock).registerLinkHandler(any());
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void getLinkHandler() {
    this.link.getLinkHandler();
    verify(linkMock).getLinkHandler();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void unregisterLinkHandler() {
    this.link.unregisterLinkHandler();
    verify(linkMock).unregisterLinkHandler();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void addLinkEventListener() {
    this.link.addLinkEventListener(null);
    verify(linkMock).addLinkEventListener(any());
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void removeLinkEventListener() {
    this.link.removeLinkEventListener(any());
    verify(linkMock).removeLinkEventListener(any());
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void connect() {
    this.link.connect();
    verify(linkMock).connect();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void disconnect() {
    this.link.disconnect();
    verify(linkMock).disconnect();
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void isConnected() {
    this.link.isConnected();
    verify(linkMock).isConnected();
    verifyNoMoreInteractions(linkMock);
  }

  //////////
  // Helpers
  //////////

  private InterledgerRejectPacket reject(InterledgerErrorCode errorCode) {
    return InterledgerRejectPacket.builder()
      .triggeredBy(InterledgerAddress.of("test.operator"))
      .code(errorCode)
      .build();
  }
}