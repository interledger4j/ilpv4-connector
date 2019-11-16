package org.interledger.connector.links.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.BalanceTracker.UpdateBalanceForFulfillResponse;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.events.OutgoingSettlementInitiationFailedEvent;
import org.interledger.connector.events.OutgoingSettlementInitiationSucceededEvent;
import org.interledger.connector.events.PacketFulfillmentEvent;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.connector.settlement.SettlementServiceException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFilter} for updating balances of the account associated to the Link being filtered.
 */
public class PublishFulfillmentLinkFilter extends AbstractLinkFilter implements LinkFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final EventBus eventBus;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} of this Connector's operator {@link InterledgerAddress}.
   * @param eventBus                An {@link EventBus}.
   */
  public PublishFulfillmentLinkFilter(
      final Supplier<InterledgerAddress> operatorAddressSupplier,
      final EventBus eventBus
  ) {
    super(operatorAddressSupplier);
    this.eventBus = Objects.requireNonNull(eventBus);
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

    responsePacket.handle(
        //////////////////////
        // If FulfillPacket...
        //////////////////////
        (interledgerFulfillPacket) -> {
          logger.debug("PUBLISHING TO EVENTBUS");
          eventBus.post(PacketFulfillmentEvent.builder()
            .accountSettings(destinationAccountSettings)
            .preparePacket(outgoingPreparePacket)
            .responsePacket(responsePacket)
            .message("response packet for " + outgoingPreparePacket.getExecutionCondition()) // FIXME what should this be?
            .build()
          );
        },
        //////////////////////
        // If Reject Packet...
        //////////////////////
        (interledgerRejectPacket) -> {
          logger.debug("NOT PUBLISHING TO EVENTBUS");
        }
    );

    // ALWAYS return the Response packet _AT ANY COST_ so this connector doesn't lose money.
    return responsePacket;
  }

}
