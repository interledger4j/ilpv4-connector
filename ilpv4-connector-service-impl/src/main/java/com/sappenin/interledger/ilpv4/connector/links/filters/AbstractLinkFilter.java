package com.sappenin.interledger.ilpv4.connector.links.filters;

import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
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
   * @param accountId    The account that the reject is coming from (in this case, the system was unable to get a
   *                     fulfill response from the target account, so the Connector is simulating a reject from that
   *                     account).
   * @param errorCode
   * @param errorMessage
   *
   * @return
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
