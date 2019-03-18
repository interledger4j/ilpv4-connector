package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
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

  private static final AccountId sourceAccountId = AccountId.of("123");
  private static final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
    .expiresAt(Instant.now())
    .destination(InterledgerAddress.of("test.dest"))
    .executionCondition(InterledgerCondition.of(new byte[32]))
    .build();

  @Mock
  private AccountRateLimitSettings rateLimitSettingsMock;

  @Mock
  private AccountSettings accountSettingsMock;

  @Mock
  private Account accountMock;

  @Mock
  private InterledgerFulfillPacket responsePacketMock;

  @Mock(answer = RETURNS_MOCKS)
  private AccountManager accountManagerMock;

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
    when(filterChainMock.doFilter(sourceAccountId, preparePacket)).thenReturn(responsePacketMock);

    // enable rate limiting by default
    when(accountManagerMock.safeGetAccount(sourceAccountId)).thenReturn(accountMock);
    when(accountMock.getAccountSettings()).thenReturn(accountSettingsMock);
    when(accountSettingsMock.getRateLimitSettings()).thenReturn(rateLimitSettingsMock);
    when(rateLimitSettingsMock.getMaxPacketsPerSecond()).thenReturn(Optional.of(1000));

    filter = new RateLimitIlpPacketFilter(() -> InterledgerAddress.of("test.operator"), accountManagerMock);

    filterWithMockLoadingCache =
      new RateLimitIlpPacketFilter(() -> InterledgerAddress.of("test.operator"), loadingCacheMock);
  }

  ///////////////////////
  // Tests with a real Loading Cache...
  ///////////////////////

  @Test
  public void doFilterWithNoAccount() {
    when(accountManagerMock.getAccount(sourceAccountId)).thenReturn(Optional.empty());

    filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    filter.doFilter(sourceAccountId, preparePacket, filterChainMock);

    verify(filterChainMock, times(3)).doFilter(sourceAccountId, preparePacket);
    verify(accountManagerMock).safeGetAccount(any()); // one time
    verifyNoMoreInteractions(accountManagerMock);
  }

  @Test
  public void doFilterWithNoMaxPacketAmount() {
    when(rateLimitSettingsMock.getMaxPacketsPerSecond()).thenReturn(Optional.empty());

    InterledgerResponsePacket response = filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(
      "Response was: " + response.getClass().getName(),
      response instanceof InterledgerFulfillPacket, is(true)
    );
    response = filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(
      "Response was: " + response.getClass().getName(),
      response instanceof InterledgerFulfillPacket, is(true)
    );

    verify(filterChainMock, times(3)).doFilter(sourceAccountId, preparePacket);
    verify(accountManagerMock).safeGetAccount(any()); // one time
    verifyNoMoreInteractions(accountManagerMock);
  }

  @Test
  public void doFilterWithInsufficientTickets() {
    when(rateLimitSettingsMock.getMaxPacketsPerSecond()).thenReturn(Optional.of(1));

    InterledgerResponsePacket response = filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));
    response = filter.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));

    verify(filterChainMock, times(1)).doFilter(sourceAccountId, preparePacket);
    verify(accountManagerMock).safeGetAccount(any()); // one time
    verifyNoMoreInteractions(accountManagerMock);
  }

  ///////////////////////
  // Tests with a Mock Loading Cache...
  ///////////////////////

  @Test
  public void doFilterWithNoPermits() throws ExecutionException {
    when(loadingCacheMock.get(sourceAccountId)).thenReturn(Optional.of(rateLimiterMock));
    when(rateLimiterMock.tryAcquire(1)).thenReturn(Boolean.FALSE);

    InterledgerResponsePacket response =
      filterWithMockLoadingCache.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));
    response = filterWithMockLoadingCache.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerRejectPacket, is(true));

    verifyZeroInteractions(filterChainMock);
    verifyZeroInteractions(accountManagerMock);
  }

  @Test
  public void doFilterWithPermit() throws ExecutionException {
    when(loadingCacheMock.get(sourceAccountId)).thenReturn(Optional.of(rateLimiterMock));
    when(rateLimiterMock.tryAcquire(1)).thenReturn(true);

    InterledgerResponsePacket response =
      filterWithMockLoadingCache.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filterWithMockLoadingCache.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));

    verify(filterChainMock, times(2)).doFilter(sourceAccountId, preparePacket);
    verifyZeroInteractions(accountManagerMock);
  }

  @Test
  public void doFilterWithNoRateLimiter() throws ExecutionException {
    when(loadingCacheMock.get(sourceAccountId)).thenReturn(Optional.empty());

    InterledgerResponsePacket response =
      filterWithMockLoadingCache.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));
    response = filterWithMockLoadingCache.doFilter(sourceAccountId, preparePacket, filterChainMock);
    assertThat(response instanceof InterledgerFulfillPacket, is(true));

    verify(filterChainMock, times(2)).doFilter(sourceAccountId, preparePacket);
    verifyZeroInteractions(accountManagerMock);
  }

}