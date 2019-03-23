package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * An abstract implementation of {@link PacketSwitchFilter} that contains common logic.
 */
public abstract class AbstractPacketFilter implements PacketSwitchFilter {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected final PacketRejector packetRejector;

  public AbstractPacketFilter(final PacketRejector packetRejector) {
    this.packetRejector = Objects.requireNonNull(packetRejector);
  }

  @VisibleForTesting
  protected InterledgerRejectPacket reject(
    final AccountId sourceAccountId, final InterledgerPreparePacket preparePacket,
    final InterledgerErrorCode errorCode, final String errorMessage
  ) {
    Objects.requireNonNull(errorCode);
    Objects.requireNonNull(errorMessage);

    logger.warn("Rejecting from {}", this.getClass().getName());
    return this.packetRejector.reject(sourceAccountId, preparePacket, errorCode, errorMessage);
  }
}
