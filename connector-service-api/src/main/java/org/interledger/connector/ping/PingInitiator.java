package org.interledger.connector.ping;

import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import com.google.common.primitives.UnsignedLong;

import java.time.Instant;
import java.util.Base64;

/**
 * <p>Defines a "ping" mechanism on a particular {@link Link}.</p>
 *
 * <p>Note that the actual business logic of making a Ping request should not start at the link because a link exists
 * on both sides of a Connector and processed information bidirectionally (i.e., links both send and receive ILPv4
 * data). Adding ping functionality to the Link layer could _also_ be used to circumvent Connector processing logic
 * (e.g., grab an outbound link and "send a ping" will not trigger any balance-change logic on the source connector).
 * Thus, this interface exists outside of the {@link Link} heirarchy, though most implementations will generally use a
 * Link to send ping requests.</p>
 *
 * @see "https://github.com/interledger/rfcs/pull/516"
 */
public interface PingInitiator {

  InterledgerCondition PING_PROTOCOL_CONDITION =
      InterledgerCondition.of(Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  /**
   * Send a very-small (or a zero) value payment to the {@code destinationAddress} and expect an ILP fulfillment, which
   * demonstrates this sender can reach the destination address using ILPv4.
   *
   * @param destinationAddress An {@link InterledgerAddress} containing the destination to ping.
   * @param pingAmount         An {@link UnsignedLong} representing the amount to use in the "ping" request.
   */
  InterledgerResponsePacket ping(InterledgerAddress destinationAddress, UnsignedLong pingAmount);

  // TODO: Implement bidirectional ping (i.e., Echo protocol) once that RFC is published.
  // See https://github.com/interledger/rfcs/pull/516

  /**
   * Construct a Ping packet with the supplied inputs.
   *
   * @param destinationAddress The {@link InterledgerAddress} that this ping packet should be delivered to.
   * @param pingAmount         An {@link UnsignedLong} with the value of this ping.
   * @param expiresAt          An {@link Instant} that indicates when this packet expires.
   *
   * @return A {@link InterledgerPreparePacket} constructed from the supplied inputs.
   */
  default InterledgerPreparePacket constructPingPacket(
      final InterledgerAddress destinationAddress,
      final UnsignedLong pingAmount,
      final Instant expiresAt
  ) {
    return InterledgerPreparePacket.builder()
        .executionCondition(PING_PROTOCOL_CONDITION)
        .expiresAt(expiresAt)
        .amount(pingAmount)
        .destination(destinationAddress)
        .build();
  }
}
