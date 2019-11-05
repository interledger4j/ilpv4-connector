package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.PacketRejector;

import com.google.common.eventbus.EventBus;

import java.util.Objects;

/**
 * An implementation of {@link PacketSwitchFilter} for handling balance updates for a given ILP request/response flow.
 */
public class PacketEventsFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  // This service is used as opposed to emitting ConnectorEvents so that we don't incur the extra object creation costs
  // multiple times per-packet (both on the incoming link and the outgoing link).
  private final MetricsService metricsService;
//  private final EventBus eventBus;

  /**
   * Required-args Constructor.
   *
   * @param packetRejector    A {@link PacketRejector}.
   * @param metricsService
   * @param eventBus          A {@link EventBus}.
   */
  public PacketEventsFilter(final PacketRejector packetRejector,
      MetricsService metricsService, final EventBus eventBus) {
    super(packetRejector);
    this.metricsService = Objects.requireNonNull(metricsService);
    //this.eventBus = Objects.requireNonNull(eventBus);
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

              // TODO: Remove commented-out event bus stuff...
              (interledgerFulfillPacket) -> {
//                eventBus.post(IncomingPacketFulfilledEvent.builder()
//                    .accountSettings(sourceAccountSettings)
//                    .incomingPacket(sourcePreparePacket)
//                    .build()
//                );
                this.metricsService.trackIncomingPacketFulfilled(sourceAccountSettings, interledgerFulfillPacket);
                return interledgerFulfillPacket;
              },
              //////////////////////
              // If Reject Packet...
              //////////////////////
              (interledgerRejectPacket) -> {
//                eventBus.post(IncomingPacketRejectedEvent.builder()
//                    .accountSettings(sourceAccountSettings)
//                    .incomingPacket(sourcePreparePacket)
//                    .build()
//                );
                this.metricsService.trackIncomingPacketRejected(sourceAccountSettings, interledgerRejectPacket);
                return interledgerRejectPacket;
              }
          );
    } catch (InterledgerProtocolException e) {
//      eventBus.post(IncomingPacketFailureEvent.builder()
//          .accountSettings(sourceAccountSettings)
//          .incomingPacket(sourcePreparePacket)
//          .build()
//      );
      this.metricsService.trackIncomingPacketFailed(sourceAccountSettings);
      throw e;
    }

  }
}
