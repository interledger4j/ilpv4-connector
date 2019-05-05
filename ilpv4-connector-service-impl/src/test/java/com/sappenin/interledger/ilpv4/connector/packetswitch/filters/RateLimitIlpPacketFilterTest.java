package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
  private AccountSettingsEntity accountSettingsMock;

  @Mock
  private InterledgerFulfillPacket responsePacketMock;

  @Mock(answer = RETURNS_MOCKS)
  private AccountSettingsRepository accountSettingsRepositoryMock;

  @Mock
  private PacketSwitchFilterChain filterChainMock;

  @Mock
  private CacheLoader<AccountId, Optional<RateLimiter>> cacheLoaderMock;

  @Mock
  private LoadingCache<AccountId, Optional<RateLimiter>> loadingCacheMock;

  @Mock
  private RateLimiter rateLimiterMock;

  private RateLimitIlpPacketFilter filter;

  private RateLimitIlpPacketFilter filterWithMockLoadingCache;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(cacheLoaderMock.load(any())).thenReturn(Optional.empty());
    when(filterChainMock.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET)).thenReturn(responsePacketMock);

    // enable rate limiting by default
    when(accountSettingsRepositoryMock.findByAccountId(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(accountSettingsMock));
    when(accountSettingsMock.getRateLimitSettings()).thenReturn(rateLimitSettingsMock);
    when(rateLimitSettingsMock.getMaxPacketsPerSecond()).thenReturn(Optional.of(1000));

    when(packetRejectorMock.reject(any(), any(), any(), any())).thenReturn(REJECT_PACKET);
    filter = new RateLimitIlpPacketFilter(packetRejectorMock, accountSettingsRepositoryMock);
    filterWithMockLoadingCache = new RateLimitIlpPacketFilter(packetRejectorMock, loadingCacheMock);
  }

  ///////////////////////
  // Tests with a real Loading Cache...
  ///////////////////////

  @Test
  public void doFilterWithNoAccount() {
    when(accountSettingsRepositoryMock.findByAccountId(SOURCE_ACCOUNT_ID)).thenReturn(Optional.empty());

    filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);

    // No need to verify this because the other tests validate the permitting.
    //verify(filterChainMock, times(3)).doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET);
    verify(accountSettingsRepositoryMock).findByAccountId(any()); // one time
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

  @Test
  public void doFilterWithNoMaxPacketAmount() {
    when(rateLimitSettingsMock.getMaxPacketsPerSecond()).thenReturn(Optional.empty());

    InterledgerResponsePacket response = filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(
      "Response was: " + response.getClass().getName(),
      response instanceof InterledgerFulfillPacket, is(true)
    );
    response = filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(
      "Response was: " + response.getClass().getName(),
      response instanceof InterledgerFulfillPacket, is(true)
    );

    verify(filterChainMock, times(3)).doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET);
    verify(accountSettingsRepositoryMock).findByAccountId(any()); // one time
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

  @Test
  public void doFilterWithInsufficientTickets() {
    when(rateLimitSettingsMock.getMaxPacketsPerSecond()).thenReturn(Optional.of(1));

    InterledgerResponsePacket response = filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));
    response = filter.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));

    verify(filterChainMock, times(1)).doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET);
    verify(accountSettingsRepositoryMock).findByAccountId(any()); // one time
    verify(packetRejectorMock, times(2)).reject(any(), any(), any(), any());
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

  ///////////////////////
  // Tests with a Mock Loading Cache...
  ///////////////////////

  @Test
  public void doFilterWithNoPermits() throws ExecutionException {
    when(loadingCacheMock.get(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(rateLimiterMock));
    when(rateLimiterMock.tryAcquire(1)).thenReturn(Boolean.FALSE);

    InterledgerResponsePacket response =
      filterWithMockLoadingCache.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));
    response = filterWithMockLoadingCache.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));

    verifyZeroInteractions(filterChainMock);
    verifyZeroInteractions(accountSettingsRepositoryMock);
  }

  @Test
  public void doFilterWithPermit() throws ExecutionException {
    when(loadingCacheMock.get(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(rateLimiterMock));
    when(rateLimiterMock.tryAcquire(1)).thenReturn(true);

    InterledgerResponsePacket response =
      filterWithMockLoadingCache.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filterWithMockLoadingCache.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));

    verify(filterChainMock, times(2)).doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET);
    verifyZeroInteractions(accountSettingsRepositoryMock);
  }

  @Test
  public void doFilterWithNoRateLimiter() throws ExecutionException {
    when(loadingCacheMock.get(SOURCE_ACCOUNT_ID)).thenReturn(Optional.empty());

    InterledgerResponsePacket response =
      filterWithMockLoadingCache.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filterWithMockLoadingCache.doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));

    verify(filterChainMock, times(2)).doFilter(SOURCE_ACCOUNT_ID, PREPARE_PACKET);
    verifyZeroInteractions(accountSettingsRepositoryMock);
  }

}