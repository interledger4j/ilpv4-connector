package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.PacketRejector;

import java.util.Objects;

/**
 * An implementation of {@link PacketSwitchFilter} for handling balance updates for a given ILP request/response flow.
 */
public class StatsPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final MetricsService metricsService;

  /**
   * Required-args Constructor.
   *
   * @param packetRejector    A {@link PacketRejector}.
   * @param metricsService A {@link MetricsService}.
   */
  public StatsPacketFilter(final PacketRejector packetRejector, final MetricsService metricsService) {
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
      return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket)
          .map(
              //////////////////////
              // If FulfillPacket...
              //////////////////////
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
    } catch (InterledgerProtocolException e) {
      this.metricsService.trackIncomingPacketFailed(sourceAccountSettings);
      throw e;
    }

  }
}
