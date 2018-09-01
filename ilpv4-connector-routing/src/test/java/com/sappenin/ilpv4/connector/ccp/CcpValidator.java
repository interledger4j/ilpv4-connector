package com.sappenin.ilpv4.connector.ccp;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRuntimeException;

import java.time.Instant;
import java.util.Objects;

import static com.sappenin.ilpv4.connector.ccp.CcpConstants.CCP_CONTROL_DESTINATION;
import static com.sappenin.ilpv4.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION;

/**
 * Helper methods to validate ILP packets with CCP Payloads.
 */
public interface CcpValidator {

  /**
   * Validate an incoming {@link InterledgerPreparePacket} whose data payload is of type {@link
   * CcpRouteControlRequest}.
   *
   * @param packet An instance of {@link InterledgerPreparePacket}.
   */
  default void validateRouteControlRequest(final InterledgerPreparePacket packet) {
    Objects.requireNonNull(packet);

    if (packet.getDestination().equals(CCP_CONTROL_DESTINATION)) {
      // TODO: Create an ILP Runtime exception that can take a packet?
      throw new InterledgerRuntimeException("Packet is not a CCP route control request");
    }

    if (!PEER_PROTOCOL_EXECUTION_CONDITION.equals(packet.getExecutionCondition())) {
      throw new InterledgerRuntimeException("Packet does not contain correct condition for a peer protocol request.");
    }

    if (Instant.now().isAfter(packet.getExpiresAt())) {
      throw new InterledgerRuntimeException("CCP route control request packet is expired.");
    }
  }

  class DefaultCcpValidator implements CcpValidator {

  }

}
