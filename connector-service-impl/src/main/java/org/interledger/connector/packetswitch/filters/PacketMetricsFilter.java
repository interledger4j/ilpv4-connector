package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.PacketRejector;

import java.util.Objects;

/**
 * An implementation of {@link PacketSwitchFilter} for handling balance updates for a given ILP request/response flow.
 */
public class PacketMetricsFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  // This service is used as opposed to emitting ConnectorEvents so that we don't incur the extra object creation costs
  // multiple times per-packet (both on the incoming link and the outgoing link).
  private final MetricsService metricsService;

  /**
   * Required-args Constructor.
   *
   * @param packetRejector A {@link PacketRejector}.
   * @param metricsService A {@link MetricsService}.
   */
  public PacketMetricsFilter(final PacketRejector packetRejector, MetricsService metricsService) {
    super(packetRejector);
    this.metricsService = Objects.requireNonNull(metricsService);
  }

  @Override
  public InterledgerResponsePacket doFilter(
      final AccountSettings sourceAccountSettings,
      final InterledgerPreparePacket sourcePreparePacket,
      final PacketSwitchFilterChain filterChain
  ) {
    try {
      this.metricsService.trackIncomingPacketPrepared(sourceAccountSettings, sourcePreparePacket);
      return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket)
          .map(
              //////////////////////
              // If FulfillPacket...
              //////////////////////

              // TODO: Remove commented-out event bus stuff...
              (interledgerFulfillPacket) -> {
                this.metricsService.trackIncomingPacketFulfilled(sourceAccountSettings, interledgerFulfillPacket);
                return interledgerFulfillPacket;
              },
              //////////////////////
              // If Reject Packet...
              //////////////////////
              (interledgerRejectPacket) -> {
                this.metricsService.trackIncomingPacketRejected(sourceAccountSettings, interledgerRejectPacket);
                return interledgerRejectPacket;
              }
          );
    } catch (Exception e) {
      this.metricsService.trackIncomingPacketFailed(sourceAccountSettings);
      throw e;
    }

  }
}
