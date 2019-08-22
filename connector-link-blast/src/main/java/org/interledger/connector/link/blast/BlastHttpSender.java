package org.interledger.connector.link.blast;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.time.Instant;

import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;

/**
 * Encapsulates how to communicate with a BLAST peer.
 */
public interface BlastHttpSender {

  // Used by BLAST to test the connection by verifying a rejection with a T01.
  InterledgerPreparePacket UNFULFILLABLE_PACKET = InterledgerPreparePacket.builder()
    .executionCondition(PING_PROTOCOL_CONDITION)
    .expiresAt(Instant.now().plusSeconds(30))
    .destination(InterledgerAddress.of("peer.ilp_over_http_connection_test_that_should_always_reject"))
    .build();

  /**
   * Send an ILP prepare packet to the remote BLAST peer.
   *
   * @param preparePacket
   *
   * @return An {@link InterledgerResponsePacket}. Note that if the request to the remote peer times-out, then the ILP
   * reject packet will contain a {@link InterledgerRejectPacket#getTriggeredBy()} that matches this node's operator
   * address.
   */
  InterledgerResponsePacket sendData(final InterledgerPreparePacket preparePacket);

  /**
   * <p>Check the `/ilp` endpoint for ping by making an HTTP Head request with a ping packet, and
   * asserting the values returned are one of the supported content-types required for BLAST.</p>
   *
   * <p>If the endpoint does not support producing BLAST responses, we expect a 406 NOT_ACCEPTABLE response. If the
   * endpoint does not support BLAST requests, then we expect a 415 UNSUPPORTED_MEDIA_TYPE.</p>
   */
  void testConnection();

}
