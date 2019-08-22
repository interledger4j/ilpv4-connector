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
 *
 * @deprecated The naming of this class is ambiguous, and likely exists in the wrong place architecturally. First, the
 * purpose of this class is to define an interface that can be used for initiating a ping; however, the naming implies
 * that implementors of this interface can accept and/or process ping requests, which is not the case. Second, ping
 * requests should generally emanate from a Sender. While it's true that a sender might be using ILP-over-HTTP, the
 * actual business logic of making a Ping request should not start at the link because, this link exists on both sides
 * of a Connector, and could be used to circumvent Connector processing logic (e.g., grab an outbound link and "send a
 * ping" will not trigger any balance-change logic on the source connector).
 */
@Deprecated
public interface PingableLink<LS extends LinkSettings> extends Link<LS> {

  InterledgerCondition PING_PROTOCOL_CONDITION =
    InterledgerCondition.of(Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

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
