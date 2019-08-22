package org.interledger.connector.links.filters;

import org.interledger.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;


/**
 * An abstract implementation of {@link PacketSwitchFilter} that contains common logic.
 */
public abstract class AbstractLinkFilter implements LinkFilter {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected final Supplier<InterledgerAddress> operatorAddressSupplier;

  public AbstractLinkFilter(
    final Supplier<InterledgerAddress> operatorAddressSupplier) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
  }

  /**
   * Helper-method to reject an incoming request.
   *
   * @param accountId     The account identifier that the reject is coming from.
   * @param preparePacket The outgoing {@link InterledgerPreparePacket} that should be sent to the Link (this is the
   *                      packet adjusted by the packet-switch containing the proper units and expiry for the outbound
   *                      Link associated to this filter).
   * @param errorCode     An {@link InterledgerErrorCode} describing why {@code preparePacket} was rejected.
   * @param errorMessage  A {@link String} providing custom details for why {@code preparePacket} was rejected.
   *
   * @return An {@link InterledgerRejectPacket} with information about why {@code preparePacket} was reject.
   */
  protected InterledgerRejectPacket reject(
    final AccountId accountId, final InterledgerPreparePacket preparePacket,
    final InterledgerErrorCode errorCode, final String errorMessage
  ) {
    Objects.requireNonNull(errorCode);
    Objects.requireNonNull(errorMessage);

    // Reject.
    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .triggeredBy(operatorAddressSupplier.get())
      .code(errorCode)
      .message(errorMessage)
      .build();

    logger.warn("Rejecting from `{}`: {} {}", accountId, preparePacket, rejectPacket);
    return rejectPacket;
  }
}
