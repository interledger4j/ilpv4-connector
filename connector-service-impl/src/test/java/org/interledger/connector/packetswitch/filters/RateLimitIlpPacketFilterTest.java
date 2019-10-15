package org.interledger.connector.packetswitch.filters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Unit tests for {@link RateLimitIlpPacketFilter}.
 */
public class RateLimitIlpPacketFilterTest {

  private static final AccountId SOURCE_ACCOUNT_ID = AccountId.of("123");

  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
    .expiresAt(Instant.now())
    .destination(InterledgerAddress.of("test.dest"))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .build();

  private static final InterledgerRejectPacket REJECT_PACKET = InterledgerRejectPacket.builder()
    .triggeredBy(InterledgerAddress.of("test.conn"))
    .code(InterledgerErrorCode.F00_BAD_REQUEST)
    .message("error message")
    .build();

  @Mock
  private PacketRejector packetRejectorMock;

  @Mock
  private AccountRateLimitSettings rateLimitSettingsMock;

  @Mock
  private AccountSettings accountSettingsMock;

  @Mock
  private InterledgerFulfillPacket responsePacketMock;

  @Mock
  private PacketSwitchFilterChain filterChainMock;

  @Mock
  private Cache<AccountId, Optional<RateLimiter>> cacheMock;

  @Mock
  private RateLimiter rateLimiterMock;

  private RateLimitIlpPacketFilter filter;

  private RateLimitIlpPacketFilter filterWithMockCache;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(filterChainMock.doFilter(accountSettingsMock, PREPARE_PACKET)).thenReturn(responsePacketMock);

    // enable rate limiting by default
    when(rateLimitSettingsMock.maxPacketsPerSecond()).thenReturn(Optional.of(1000));

    when(accountSettingsMock.accountId()).thenReturn(SOURCE_ACCOUNT_ID);
    when(accountSettingsMock.maximumPacketAmount()).thenReturn(Optional.empty());
    when(accountSettingsMock.rateLimitSettings()).thenReturn(rateLimitSettingsMock);

    when(packetRejectorMock.reject(any(), any(), any(), any())).thenReturn(REJECT_PACKET);
    filter = new RateLimitIlpPacketFilter(packetRejectorMock);
    filterWithMockCache = new RateLimitIlpPacketFilter(packetRejectorMock, cacheMock);
  }

  ///////////////////////
  // Tests with a real Loading Cache...
  ///////////////////////

  @Test
  public void doFilterWithNoAccount() {
    filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
  }

  @Test
  public void doFilterWithNoMaxPacketAmount() {
    when(rateLimitSettingsMock.maxPacketsPerSecond()).thenReturn(Optional.empty());

    InterledgerResponsePacket response = filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(
      "Response was: " + response.getClass().getName(),
      response instanceof InterledgerFulfillPacket, is(true)
    );
    response = filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(
      "Response was: " + response.getClass().getName(),
      response instanceof InterledgerFulfillPacket, is(true)
    );

    verify(filterChainMock, times(3)).doFilter(accountSettingsMock, PREPARE_PACKET);
  }

  @Test
  public void doFilterWithInsufficientTickets() {
    when(rateLimitSettingsMock.maxPacketsPerSecond()).thenReturn(Optional.of(1));

    InterledgerResponsePacket response = filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));
    response = filter.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));

    verify(filterChainMock, times(1)).doFilter(accountSettingsMock, PREPARE_PACKET);
    verify(packetRejectorMock, times(2)).reject(any(), any(), any(), any());
  }

  ///////////////////////
  // Tests with a Mock Loading Cache...
  ///////////////////////

  @Test
  public void doFilterWithNoPermits() throws ExecutionException {
    when(cacheMock.get(any(), any())).thenReturn(Optional.of(rateLimiterMock));

    when(rateLimiterMock.tryAcquire(1)).thenReturn(Boolean.FALSE);

    InterledgerResponsePacket response =
      filterWithMockCache.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));
    response = filterWithMockCache.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));

    verifyZeroInteractions(filterChainMock);
  }

  @Test
  public void doFilterWithPermit() throws ExecutionException {
    when(cacheMock.get(any(), any())).thenReturn(Optional.of(rateLimiterMock));
    when(rateLimiterMock.tryAcquire(1)).thenReturn(true);

    InterledgerResponsePacket response =
      filterWithMockCache.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filterWithMockCache.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));

    verify(filterChainMock, times(2)).doFilter(accountSettingsMock, PREPARE_PACKET);
  }

  @Test
  public void doFilterWithNoRateLimiter() throws ExecutionException {
    when(cacheMock.get(any(), any())).thenReturn(Optional.empty());

    InterledgerResponsePacket response =
      filterWithMockCache.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filterWithMockCache.doFilter(accountSettingsMock, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));

    verify(filterChainMock, times(2)).doFilter(accountSettingsMock, PREPARE_PACKET);
  }

}
