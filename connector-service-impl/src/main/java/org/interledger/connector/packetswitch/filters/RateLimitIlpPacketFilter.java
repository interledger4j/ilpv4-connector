package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LinkId;
import org.interledger.link.PacketRejector;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>An implementation of {@link PacketSwitchFilter} for limiting per-account traffic on this connector.</p>
 *
 * <p>This implementation uses a combination of a {@link RateLimiter} per-account, combined with a
 * {@link Cache} that expires after 15 seconds. In this way, accounts that do not send data to this Connector instance
 * will not have an active rate-limiter in memory.
 * </p>
 */
@SuppressWarnings("UnstableApiUsage")
public class RateLimitIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final MetricsService metricsService;

  // Account-based rate-limiters.
  private final Cache<AccountId, Optional<RateLimiter>> rateLimiters;

  /**
   * Required-args Constructor; exists only for testing.
   *
   * @param packetRejector    A {@link PacketRejector}.
   * @param metricsService A {@link MetricsService}.
   * @param rateLimiterCache  A {@link Cache} for rate-limiting decisions.
   */
  public RateLimitIlpPacketFilter(
      final PacketRejector packetRejector,
      final MetricsService metricsService,
      final Cache<AccountId, Optional<RateLimiter>> rateLimiterCache
  ) {
    super(packetRejector);
    this.metricsService = Objects.requireNonNull(metricsService);
    this.rateLimiters = Objects.requireNonNull(rateLimiterCache);
  }

  @Override
  public InterledgerResponsePacket doFilter(
      final AccountSettings sourceAccountSettings,
      final InterledgerPreparePacket sourcePreparePacket,
      final PacketSwitchFilterChain filterChain
  ) {
    return rateLimiters
        .get(
            sourceAccountSettings.accountId(),
            (key) -> sourceAccountSettings.rateLimitSettings().maxPacketsPerSecond().map(RateLimiter::create)
        )
        .map(rateLimiter -> {
          if (rateLimiter.tryAcquire(1)) {
            return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
          } else {
            this.metricsService.trackNumRateLimitedPackets(sourceAccountSettings);
            return packetRejector.reject(
                LinkId.of(sourceAccountSettings.accountId().value()),
                sourcePreparePacket,
                InterledgerErrorCode.T03_CONNECTOR_BUSY,
                "Rate Limit exceeded"
            );
          }
        })
        // There is no RateLimiter for this account (because RateLimiting is disabled) so simply continue the
        // FilterChain.
        .orElseGet(() -> filterChain.doFilter(sourceAccountSettings, sourcePreparePacket));
  }
}
