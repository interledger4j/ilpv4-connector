package org.interledger.connector.links.filters;

import static org.interledger.core.InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerRuntimeException;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.PacketRejector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The default implementation of {@link LinkFilterChain} that enforces balance and expiry, regardless of what a
 * developer does in the filter chain.
 */
public class DefaultLinkFilterChain implements LinkFilterChain {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLinkFilterChain.class);
  private static final Executor EXECUTOR = Executors.newCachedThreadPool();

  private final PacketRejector packetRejector;
  private final List<LinkFilter> linkFilters;
  private final Link link;
  // The index of the filter to call next...
  private int _filterIndex;

  /**
   * A chain of filters that are applied to a packet request before sending the packet onto an outbound {@link Link}.
   *
   * @param packetRejector A {@link PacketRejector} used to reject packets.
   * @param linkFilters    A {@link List} of Link filters that should be applied to this filter chain.
   * @param outboundLink   The {@link Link} that a Packet Switch will forward a packet onto (this link is the
   *                       `next-hop`
   */
  public DefaultLinkFilterChain(
    final PacketRejector packetRejector,
    final List<LinkFilter> linkFilters,
    final Link outboundLink
  ) {
    this.packetRejector = Objects.requireNonNull(packetRejector);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.link = Objects.requireNonNull(outboundLink);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings destinationAccountSettings, final InterledgerPreparePacket preparePacket
  ) {

    Objects.requireNonNull(destinationAccountSettings);
    Objects.requireNonNull(preparePacket);

    try {
      if (this._filterIndex < this.linkFilters.size()) {
        return linkFilters.get(_filterIndex++).doFilter(destinationAccountSettings, preparePacket, this);
      } else {

        try {
          LOGGER.debug(
            "Sending outbound ILP Prepare. destinationAccountSettings: {}; link={}; packet={};",
            destinationAccountSettings, link, preparePacket
          );

          // Expiry timeout is handled here (not in a filter) on the premise that a filter can fail or allow a
          // developer to abort the filter pipeline, but we never want to allow a developer to accidentally do this so
          // that expiry handling of an outgoing request is always enforced.
          final Duration timeoutDuration = Duration.between(Instant.now(), preparePacket.getExpiresAt());
          // `timeoutDuration` can be negative, so need to perform this check here to make sure we don't send a
          // negative or 0 timeout into the completable future.
          if (timeoutDuration.isNegative() || timeoutDuration.isZero()) {
            return packetRejector.reject(
              LinkId.of(destinationAccountSettings.accountId().value()),
              preparePacket,
              R02_INSUFFICIENT_TIMEOUT,
              "The connector could not forward the payment, because the timeout was too low"
            );
          }

          return CompletableFuture.supplyAsync(() -> link.sendPacket(preparePacket), EXECUTOR)
            .get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);

        } catch (InterruptedException | ExecutionException e) {
          LOGGER.error(e.getMessage(), e);
          return packetRejector.reject(
            LinkId.of(destinationAccountSettings.accountId().value()),
            preparePacket,
            InterledgerErrorCode.T00_INTERNAL_ERROR,
            String.format("Internal Error: %s", e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
          );
        } catch (TimeoutException e) {
          LOGGER.error(e.getMessage(), e);
          return packetRejector.reject(
            LinkId.of(destinationAccountSettings.accountId().value()),
            preparePacket,
            InterledgerErrorCode.R00_TRANSFER_TIMED_OUT,
            "Transfer Timed-out"
          );
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
          return packetRejector.reject(
            LinkId.of(destinationAccountSettings.accountId().value()),
            preparePacket,
            InterledgerErrorCode.T00_INTERNAL_ERROR,
            String.format("Internal Error: %s", e.getMessage())
          );
        }
      }
    } catch (Exception e) {
      // If anything emits an uncaught exception, this is considered a failure case. These always translate into a
      // rejection.
      LOGGER.error("Failure in LinkFilterChain: " + e.getMessage(), e);
      if (InterledgerRuntimeException.class.isAssignableFrom(e.getClass())) {
        return ((InterledgerProtocolException) e).getInterledgerRejectPacket();
      } else {
        return packetRejector.reject(
          LinkId.of(destinationAccountSettings.accountId().value()),
          preparePacket,
          InterledgerErrorCode.T00_INTERNAL_ERROR,
          String.format("Internal Error: %s", e.getMessage())
        );
      }
    }
  }
}
