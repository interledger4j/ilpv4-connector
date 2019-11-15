package org.interledger.connector.links.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings.Builder;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Unit tests for {@link OutgoingMetricsLinkFilter}.
 */
public class OutgoingMetricsLinkFilterTest {

  private static final Supplier<InterledgerAddress> OPERATOR_ADDRESS = () -> InterledgerAddress.of("example.operator");
  private static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("example.destination");

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private LinkFilterChain filterChainMock;
  @Mock
  private PacketRejector packetRejectorMock;
  @Mock
  private MetricsService metricsServiceMock;

  private OutgoingMetricsLinkFilter filter;

  @Before
  public void setUp() {
    filter = new OutgoingMetricsLinkFilter(OPERATOR_ADDRESS, metricsServiceMock);
  }

  @Test
  public void doFilterWithFulfill() {
    when(filterChainMock.doFilter(accountSettings(), preparePacket())).thenReturn(fulfillPacket());

    final InterledgerResponsePacket actual = filter.doFilter(accountSettings(), preparePacket(), filterChainMock);

    assertThat(actual).isEqualTo(fulfillPacket());

    verifyNoMoreInteractions(packetRejectorMock);
    verify(metricsServiceMock).trackOutgoingPacketPrepared(accountSettings(), preparePacket());
    verify(metricsServiceMock).trackOutgoingPacketFulfilled(accountSettings(), fulfillPacket());
    verifyNoMoreInteractions(metricsServiceMock);
  }

  @Test
  public void doFilterWithReject() {
    when(filterChainMock.doFilter(accountSettings(), preparePacket())).thenReturn(rejectPacket());

    final InterledgerResponsePacket actual = filter.doFilter(accountSettings(), preparePacket(), filterChainMock);

    assertThat(actual).isEqualTo(rejectPacket());

    verifyNoMoreInteractions(packetRejectorMock);
    verify(metricsServiceMock).trackOutgoingPacketPrepared(accountSettings(), preparePacket());
    verify(metricsServiceMock).trackOutgoingPacketRejected(accountSettings(), rejectPacket());
    verifyNoMoreInteractions(metricsServiceMock);
  }

  @Test
  public void doFilterWithInterledgerProtocolException() {
    final AccountSettings accountSettings = accountSettings();
    final InterledgerPreparePacket preparePacket = preparePacket();
    expectedException.expect(InterledgerProtocolException.class);
    expectedException.expectMessage("Interledger Rejection: ");

    doThrow(new InterledgerProtocolException(rejectPacket()))
        .when(filterChainMock)
        .doFilter(eq(accountSettings), eq(preparePacket));

    try {
      filter.doFilter(accountSettings, preparePacket, filterChainMock);
      fail("cannot run doFilter");
    } catch (Exception e) {
      verifyNoMoreInteractions(packetRejectorMock);
      verify(metricsServiceMock).trackOutgoingPacketPrepared(accountSettings, preparePacket);
      verify(metricsServiceMock).trackOutgoingPacketRejected(accountSettings, rejectPacket());
      verifyNoMoreInteractions(metricsServiceMock);
      throw e;
    }
  }

  @Test
  public void doFilterWithException() {
    final AccountSettings accountSettings = accountSettings();
    final InterledgerPreparePacket preparePacket = preparePacket();
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("foo");

    doThrow(new RuntimeException("foo")).when(filterChainMock).doFilter(eq(accountSettings), eq(preparePacket));

    try {
      filter.doFilter(accountSettings, preparePacket, filterChainMock);
      fail("cannot run doFilter");
    } catch (Exception e) {
      verifyNoMoreInteractions(packetRejectorMock);
      verify(metricsServiceMock).trackOutgoingPacketPrepared(accountSettings, preparePacket);
      verify(metricsServiceMock).trackOutgoingPacketFailed(accountSettings);
      verifyNoMoreInteractions(metricsServiceMock);
      throw e;
    }
  }

  //////////////////
  // Private Helpers
  //////////////////

  private Builder accountSettingsBuilder() {
    return AccountSettings.builder()
        .accountId(AccountId.of("testAccountId"))
        .accountRelationship(AccountRelationship.PEER)
        .assetScale(9)
        .assetCode("XRP")
        .linkType(LoopbackLink.LINK_TYPE);
  }

  private AccountSettings accountSettings() {
    return accountSettingsBuilder().build();
  }

  private InterledgerPreparePacket preparePacket() {
    return preparePacket(UnsignedLong.ZERO);
  }

  private InterledgerPreparePacket preparePacket(final UnsignedLong amount) {
    return InterledgerPreparePacket.builder()
        .destination(DESTINATION_ADDRESS)
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .amount(amount)
        .expiresAt(Instant.MAX)
        .build();
  }

  private InterledgerFulfillPacket fulfillPacket() {
    return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
  }

  private InterledgerRejectPacket rejectPacket() {
    return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
        .build();
  }
}
