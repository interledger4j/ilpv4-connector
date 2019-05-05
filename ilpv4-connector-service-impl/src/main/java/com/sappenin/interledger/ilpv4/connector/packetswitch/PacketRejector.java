package com.sappenin.interledger.ilpv4.connector.packetswitch;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utility class for helping to reject a packet.
 */
public class PacketRejector {

  private static final InterledgerAddress UNSET_OPERATOR_ADDRESS =
    InterledgerAddress.of(InterledgerAddress.AllocationScheme.PRIVATE.getValue() + ".unset-operator-address");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  /**
   * Required-args Constructor.
   */
  public PacketRejector(final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier) {
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
  public InterledgerRejectPacket reject(
    final AccountId sourceAccountId, final InterledgerPreparePacket preparePacket,
    final InterledgerErrorCode errorCode, final String errorMessage
  ) {
    Objects.requireNonNull(errorCode);
    Objects.requireNonNull(errorMessage);

    // Reject.
    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .triggeredBy(operatorAddressSupplier.get().orElse(UNSET_OPERATOR_ADDRESS))
      .code(errorCode)
      .message(errorMessage)
      .build();

    logger.warn("Rejecting Prepare Packet from `{}`: {} {}", sourceAccountId, preparePacket, rejectPacket);
    return rejectPacket;
  }

}
