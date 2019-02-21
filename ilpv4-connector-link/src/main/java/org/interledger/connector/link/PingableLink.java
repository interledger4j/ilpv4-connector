package org.interledger.connector.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Defines a "ping" mechanism on a particular {@link Link}.
 */
public interface PingableLink<LS extends LinkSettings> extends Link<LS> {

  InterledgerCondition PING_PROTOCOL_CONDITION =
    InterledgerCondition.of(Base64.getDecoder().decode("Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU="));

  /**
   * Send a very-small value payment to the destination and expect an ILP fulfillment, which demonstrates this sender
   * has send-data ping to the indicated destination address.
   *
   * @param destinationAddress
   */
  default InterledgerResponsePacket ping(final InterledgerAddress destinationAddress, final BigInteger pingAmount) {
    Objects.requireNonNull(destinationAddress);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
      .executionCondition(PING_PROTOCOL_CONDITION)
      // TODO: Make this timeout configurable!
      .expiresAt(Instant.now().plusSeconds(30))
      .amount(pingAmount)
      .destination(destinationAddress)
      .build();

    return this.sendPacket(pingPacket);
  }
}
