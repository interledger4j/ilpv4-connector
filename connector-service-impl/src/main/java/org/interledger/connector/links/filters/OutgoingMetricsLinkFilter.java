package org.interledger.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.metrics.MetricsService;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFilter} for updating balances of the account associated to the Link being filtered.
 */
public class OutgoingMetricsLinkFilter extends AbstractLinkFilter implements LinkFilter {

  private final MetricsService metricsService;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} of this Connector's operator {@link InterledgerAddress}.
   * @param metricsService          A {@link MetricsService}.
   */
  public OutgoingMetricsLinkFilter(
      final Supplier<InterledgerAddress> operatorAddressSupplier, final MetricsService metricsService
  ) {
    super(operatorAddressSupplier);
    this.metricsService = Objects.requireNonNull(metricsService);
  }

  @Override
  public InterledgerResponsePacket doFilter(
      final AccountSettings destinationAccountSettings,
      final InterledgerPreparePacket outgoingPreparePacket,
      final LinkFilterChain filterChain
  ) {
    Objects.requireNonNull(destinationAccountSettings);
    Objects.requireNonNull(outgoingPreparePacket);
    Objects.requireNonNull(filterChain);

    try {
      this.metricsService.trackOutgoingPacketPrepared(destinationAccountSettings, outgoingPreparePacket);
      return filterChain.doFilter(destinationAccountSettings, outgoingPreparePacket)
          .map(
              //////////////////////
              // If FulfillPacket...
              //////////////////////
              (interledgerFulfillPacket) -> {
                metricsService.trackOutgoingPacketFulfilled(destinationAccountSettings, interledgerFulfillPacket);
                return interledgerFulfillPacket;
              },
              //////////////////////
              // If Reject Packet...
              //////////////////////
              (interledgerRejectPacket) -> {
                metricsService.trackOutgoingPacketRejected(destinationAccountSettings, interledgerRejectPacket);
                return interledgerRejectPacket;
              }
          );
    } catch (InterledgerProtocolException e) {
      this.metricsService.trackOutgoingPacketRejected(destinationAccountSettings, e.getInterledgerRejectPacket());
      throw e;
    } catch (Exception e) {
      metricsService.trackOutgoingPacketFailed(destinationAccountSettings);
      throw e;
    }
  }
}
