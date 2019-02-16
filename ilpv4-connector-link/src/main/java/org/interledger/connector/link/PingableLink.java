package org.interledger.connector.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines a "ping" mechanism on a particular {@link Link}.
 */
public interface PingableLink extends Link {

  InterledgerCondition PING_PROTOCOL_CONDITION =
    InterledgerCondition.of(Base64.getDecoder().decode("Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU="));

  /**
   * Send a very-small value payment to the destination and expect an ILP fulfillment, which demonstrates this sender
   * has send-data connectivity to the indicated destination address.
   *
   * @param destinationAddress
   */
  default Optional<InterledgerResponsePacket> ping(final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      // TODO: Make this timeout configurable!
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(BigInteger.ZERO)
      .destination(destinationAddress)
      .build();

    return this.sendPacket(pingPacket);
  }
}
