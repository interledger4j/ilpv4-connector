package org.interledger.connector.link;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;

import com.google.common.primitives.UnsignedLong;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests {@link CircuitBreakingLink}.
 */
public class CircuitBreakingLinkTest {

  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .destination(InterledgerAddress.of("example.recipient"))
    .amount(UnsignedLong.valueOf(10))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .expiresAt(Instant.now().plusSeconds(50))
    .build();

  private static final String LINK_ID_VALUE = "123";
  private static final LinkId LINK_ID = LinkId.of(LINK_ID_VALUE);
  private static final CircuitBreakerConfig CONFIG = CircuitBreakerConfig.custom()
    .failureRateThreshold(2)
    .slidingWindow(2, 2, SlidingWindowType.COUNT_BASED)
    .ignoreExceptions(InterledgerProtocolException.class)
    .enableAutomaticTransitionFromOpenToHalfOpen()
    .build();

  @Mock
  private Link<LinkSettings> linkDelegateMock;

  @Mock
  private LinkSettings delegateLinkSettingsMock;

  private CircuitBreakingLink link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(linkDelegateMock.getLinkId()).thenReturn(LINK_ID);
    when(linkDelegateMock.getOperatorAddressSupplier()).thenReturn(() -> InterledgerAddress.of("example.operator"));
    when(linkDelegateMock.getLinkSettings()).thenReturn(delegateLinkSettingsMock);
    when(linkDelegateMock.sendPacket(PREPARE_PACKET)).thenReturn(reject(InterledgerErrorCode.T03_CONNECTOR_BUSY));

    this.link = new CircuitBreakingLink(linkDelegateMock, CONFIG);
  }

  @Test
  public void sendRecordIlpExceptions() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID_VALUE);

    // Simulate a failure attempt
    circuitBreaker.onError(0, TimeUnit.SECONDS,
      new InterledgerProtocolException(reject(InterledgerErrorCode.T02_PEER_BUSY)));
    // CircuitBreaker is still CLOSED, because 1 failure is allowed
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    // Simulate a 2nd failure attempt
    circuitBreaker.onError(0, TimeUnit.SECONDS,
      new InterledgerProtocolException(reject(InterledgerErrorCode.T03_CONNECTOR_BUSY)));
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    // Simulate a 3rd failure attempt
    circuitBreaker
      .onError(0, TimeUnit.SECONDS, new InterledgerProtocolException(reject(InterledgerErrorCode.T02_PEER_BUSY)));
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  public void sendRecordNonIlpExceptions() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID_VALUE);

    // Simulate a failure attempt
    circuitBreaker.onError(0, TimeUnit.SECONDS, new RuntimeException("foo"));
    // CircuitBreaker is still CLOSED, because 1 failure is allowed
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    // Simulate a 2nd failure attempt
    circuitBreaker.onError(0, TimeUnit.SECONDS, new RuntimeException("foo"));
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    // Simulate a 3rd failure attempt
    circuitBreaker.onError(0, TimeUnit.SECONDS, new RuntimeException("foo"));
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }


  @Test
  public void sendPacketWithIlpExceptionsBelowThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID_VALUE);

    // Simulate a failure attempt
    CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
      CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
    Try<InterledgerResponsePacket> result = Try.of(() -> function.apply(PREPARE_PACKET));

    //Then
    // CircuitBreaker is still CLOSED, because 1 Reject has not been recorded as a failure
    assertThat(result.isFailure()).isFalse();
    assertThat(result.failed().isEmpty()).isTrue();
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  public void sendPacketWithIlpExceptionsAboveThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID_VALUE);

    Try<InterledgerResponsePacket> result = null;
    for (int i = 0; i < 2; i++) {
      //When
      CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
        CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
      result = Try.of(() -> function.apply(PREPARE_PACKET));
    }

    //Then
    assertThat(result.isFailure()).isFalse();
    assertThat(result.failed().isEmpty()).isTrue();
    // CircuitBreaker is still CLOSED, because 1 Reject has been recorded as a failure
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  public void sendPacketWithNumFailuresBelowThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID_VALUE);

    doThrow(new RuntimeException("foo")).when(linkDelegateMock).sendPacket(any());

    // Simulate a failure attempts
    CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
      CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
    Try<InterledgerResponsePacket> result = Try.of(() -> function.apply(PREPARE_PACKET));

    //Then
    assertThat(result.isFailure()).isTrue();
    // CircuitBreaker is still CLOSED, because 1 Reject has not been recorded as a failure
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    assertThat(result.failed().get().getClass().toString()).isEqualTo(RuntimeException.class.toString());
  }

  @Test
  public void sendPacketWithNumFailuresAboveThreshold() {
    final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CONFIG).circuitBreaker(LINK_ID_VALUE);
    doThrow(new RuntimeException("foo")).when(linkDelegateMock).sendPacket(any());

    Try<InterledgerResponsePacket> result = null;
    for (int i = 0; i < 2; i++) {
      //When
      CheckedFunction1<InterledgerPreparePacket, InterledgerResponsePacket> function =
        CircuitBreaker.decorateCheckedFunction(circuitBreaker, (preparePacket) -> link.sendPacket(PREPARE_PACKET));
      result = Try.of(() -> function.apply(PREPARE_PACKET));
    }

    //Then
    assertThat(result.isFailure()).isTrue();
    // CircuitBreaker is still CLOSED, because 1 Reject has been recorded as a failure
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(result.failed().get().getClass().getName()).isEqualTo(RuntimeException.class.getName());
  }

  @Test
  public void getLinkId() {
    this.link.setLinkId(LINK_ID);
    this.link.getLinkId();
    verify(linkDelegateMock).getOperatorAddressSupplier();
    verify(linkDelegateMock).getLinkSettings();
    verify(linkDelegateMock).getLinkId();
    verify(linkDelegateMock).setLinkId(any());
    verifyNoMoreInteractions(linkDelegateMock);
  }

  @Test
  public void getOperatorAddressSupplier() {
    this.link.getOperatorAddressSupplier();
    verify(linkDelegateMock, times(2)).getOperatorAddressSupplier();
    verify(linkDelegateMock).getLinkSettings();
    verifyNoMoreInteractions(linkDelegateMock);
  }

  @Test
  public void getLinkSettings() {
    this.link.getLinkSettings();
    verify(linkDelegateMock).getOperatorAddressSupplier();
    verify(linkDelegateMock, times(2)).getLinkSettings();
    verifyNoMoreInteractions(linkDelegateMock);
  }

  @Test
  public void registerLinkHandler() {
    this.link.registerLinkHandler(null);
    verify(linkDelegateMock).getOperatorAddressSupplier();
    verify(linkDelegateMock).getLinkSettings();
    verify(linkDelegateMock).registerLinkHandler(any());
    verifyNoMoreInteractions(linkDelegateMock);
  }

  @Test
  public void getLinkHandler() {
    assertThat(link.getLinkHandler()).isEqualTo(Optional.empty());
    verify(linkDelegateMock).getOperatorAddressSupplier();
    verify(linkDelegateMock).getLinkSettings();
    verify(linkDelegateMock).getLinkHandler();
    verifyNoMoreInteractions(linkDelegateMock);
  }

  @Test
  public void unregisterLinkHandler() {
    this.link.unregisterLinkHandler();
    verify(linkDelegateMock).getOperatorAddressSupplier();
    verify(linkDelegateMock).getLinkSettings();
    verify(linkDelegateMock).unregisterLinkHandler();
    verifyNoMoreInteractions(linkDelegateMock);
  }

  //////////
  // Helpers
  //////////

  private InterledgerRejectPacket reject(InterledgerErrorCode errorCode) {
    return InterledgerRejectPacket.builder()
      .triggeredBy(InterledgerAddress.of("example.operator"))
      .code(errorCode)
      .build();
  }
}
