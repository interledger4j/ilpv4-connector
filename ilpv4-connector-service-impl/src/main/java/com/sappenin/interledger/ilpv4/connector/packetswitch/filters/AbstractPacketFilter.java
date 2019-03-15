package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

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
public abstract class AbstractPacketFilter implements PacketSwitchFilter {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected final Supplier<InterledgerAddress> operatorAddressSupplier;

  public AbstractPacketFilter(final Supplier<InterledgerAddress> operatorAddressSupplier) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
  }

  /**
   * Helper-method to reject an incoming request.
   *
   * @param errorCode
   * @param errorMessage
   *
   * @return
   */
  protected InterledgerRejectPacket reject(
    final AccountId sourceAccountId, final InterledgerPreparePacket preparePacket,
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

    logger.warn("Rejecting Prepare Packet from `{}`: {} {}", sourceAccountId, preparePacket, rejectPacket);
    return rejectPacket;
  }
}
