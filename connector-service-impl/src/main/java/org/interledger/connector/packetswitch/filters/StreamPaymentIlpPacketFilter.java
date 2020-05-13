package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.connector.payments.FulfillmentGeneratedEventAggregator;
import org.interledger.connector.payments.StreamPaymentType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.PacketRejector;
import org.interledger.stream.Denomination;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * An implementation of {@link PacketSwitchFilter} for handling balance updates for a given ILP request/response flow.
 */
public class StreamPaymentIlpPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  private final FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator;

  public StreamPaymentIlpPacketFilter(final PacketRejector packetRejector,
                                      final FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator) {
    super(packetRejector);
    this.fulfillmentGeneratedEventAggregator = Objects.requireNonNull(fulfillmentGeneratedEventAggregator);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
    ) {
    return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket).map(
      //////////////////////
      // If FulfillPacket...
      //////////////////////
      (interledgerFulfillPacket) -> {
        if (UnsignedLong.ZERO.equals(sourcePreparePacket.getAmount())) {
          // No need to track 0-value packets...
          return interledgerFulfillPacket;
        }
        // If a packet is fulfilled, then the Receiver's balance is always adjusted in the outgoing LinkFilter, so
        // there's nothing to do here.
        fulfillmentGeneratedEventAggregator.aggregate(FulfillmentGeneratedEvent.builder()
          .accountId(sourceAccountSettings.accountId())
          .denomination(Denomination.builder()
            .assetScale((short) sourceAccountSettings.assetScale())
            .assetCode(sourceAccountSettings.assetCode())
            .build()
          )
          .preparePacket(sourcePreparePacket)
          .fulfillPacket(interledgerFulfillPacket)
          .paymentType(StreamPaymentType.PAYMENT_SENT)
          .build()
        );
        return interledgerFulfillPacket;
      },
      //////////////////////
      // If Reject Packet...
      //////////////////////
      (interledgerRejectPacket) -> {
        // nothing to do here. rejections are not currently tracked in the database on stream payments.
        return interledgerRejectPacket;
      }
    );
  }

}