package org.interledger.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.events.FulfillmentGeneratedEvent;
import org.interledger.connector.payments.FulfillmentGeneratedEventAggregator;
import org.interledger.connector.payments.StreamPaymentType;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.Denomination;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFilter} for updating balances of the account associated to the Link being filtered.
 */
public class OutgoingStreamPaymentLinkFilter extends AbstractLinkFilter implements LinkFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} of this Connector's operator {@link InterledgerAddress}.
   * @param fulfillmentGeneratedEventAggregator
   */
  public OutgoingStreamPaymentLinkFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier,
    FulfillmentGeneratedEventAggregator fulfillmentGeneratedEventAggregator) {
    super(operatorAddressSupplier);
    this.fulfillmentGeneratedEventAggregator = fulfillmentGeneratedEventAggregator;
  }

  @Override
  public InterledgerResponsePacket doFilter(
      final AccountSettings destinationAccountSettings,
      final InterledgerPreparePacket outgoingPreparePacket,
      final LinkFilterChain filterChain
  ) {
    Objects.requireNonNull(destinationAccountSettings, "destinationAccountSettings must not be null");
    Objects.requireNonNull(outgoingPreparePacket, "outgoingPreparePacket must not be null");
    Objects.requireNonNull(filterChain, "filterChain must not be null");

    final InterledgerResponsePacket responsePacket
        = filterChain.doFilter(destinationAccountSettings, outgoingPreparePacket);
    try {
      return responsePacket.handleAndReturn(
        //////////////////////
        // If FulfillPacket...
        //////////////////////
        (interledgerFulfillPacket) -> {
          if (UnsignedLong.ZERO.equals(outgoingPreparePacket.getAmount())) {
            // No need to track 0-value packets...
            return;
          }
          fulfillmentGeneratedEventAggregator.aggregate(FulfillmentGeneratedEvent.builder()
            .accountId(destinationAccountSettings.accountId())
            .denomination(Denomination.builder()
              .assetScale((short) destinationAccountSettings.assetScale())
              .assetCode(destinationAccountSettings.assetCode())
              .build()
            )
            .preparePacket(outgoingPreparePacket)
            .fulfillPacket(interledgerFulfillPacket)
            .paymentType(StreamPaymentType.PAYMENT_RECEIVED)
            .build()
          );
        },
        //////////////////////
        // only fulfillments are tracked on stream payments. nothing to do on reject.
        //////////////////////
        (interledgerRejectPacket) ->{}
      );
    } catch (Exception e) {
      logger.error("Failed to aggregate fulfillment. Packet will still be fulfilled by payment tracking data will" +
        "be out of sync with balance tracking. Prepare={}, Response={}", outgoingPreparePacket, responsePacket, e);
      return responsePacket;
    }
  }

}
