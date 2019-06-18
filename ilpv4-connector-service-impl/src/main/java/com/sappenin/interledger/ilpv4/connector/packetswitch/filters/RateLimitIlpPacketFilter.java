package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * <p>An implementation of {@link PacketSwitchFilter} for limiting per-account traffic on this connector.</p>
 *
 * <p>This implementation uses a combination of a {@link RateLimiter} per-account, combined with a
 * {@link LoadingCache} that expires after 15 seconds. In this way, accounts that do not send data to this Connector
 * instance will not have an active rate-limiter in memory.
 * </p>
 */
public class RateLimitIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  // Account-based rate-limiters.
  private final Cache<AccountId, Optional<RateLimiter>> rateLimiters;

  /**
   * Required-args Constructor.
   */
  public RateLimitIlpPacketFilter(
    final PacketRejector packetRejector
  ) {
    this(packetRejector, CacheBuilder.newBuilder()
      //.maximumSize(100) // Not enabled for now in order to support many accounts.
      .expireAfterAccess(30, TimeUnit.SECONDS)
      .build()
    );
  }

  /**
   * Required-args Constructor; exists only for testing.
   */
  @VisibleForTesting
  RateLimitIlpPacketFilter(
    final PacketRejector packetRejector,
    final Cache<AccountId, Optional<RateLimiter>> rateLimiterCache
  ) {
    super(packetRejector);
    this.rateLimiters = Objects.requireNonNull(rateLimiterCache);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    try {
      return rateLimiters
        .get(sourceAccountSettings.getAccountId(),
          () -> sourceAccountSettings.getRateLimitSettings().getMaxPacketsPerSecond()
            .map(packetsPerSecond -> RateLimiter.create(packetsPerSecond))
        )
        .map(rateLimiter -> {
          if (rateLimiter.tryAcquire(1)) {
            return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
          } else {
            return packetRejector.reject(
              sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.T03_CONNECTOR_BUSY,
              "Rate Limit exceeded"
            );
          }
        })
        // There is no RateLimiter for this account (because RateLimiting is disabled) so simply continue the
        // FilterChain.
        .orElseGet(() -> filterChain.doFilter(sourceAccountSettings, sourcePreparePacket));
    } catch (ExecutionException e) {
      throw new RuntimeException(e); // Should map to T99 Internal Error.
    }

  }
}
