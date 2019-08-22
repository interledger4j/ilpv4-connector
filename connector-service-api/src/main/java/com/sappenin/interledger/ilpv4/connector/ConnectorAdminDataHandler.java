package com.sappenin.interledger.ilpv4.connector;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Defines how to send data from a particular Connector account into a Connector.
 */
@FunctionalInterface
public
interface ConnectorAdminDataHandler {

  /**
   * <p>Handles an incoming {@link InterledgerPreparePacket} received from a peer via a connected plugin. In other
   * words, whenever a plugin gets an incoming packet, it MUST call this method with all relevant information in order
   * to properly process the packet.</p>
   *
   * <p>Implementation of this method should generally forward this call to the packet-switch, but may introduce
   * custom routing logic, such as for handling packets that should not be packet-switched.</p>
   *
   * @param accountId             An {@link AccountId} for the remote peer (directly connected to this Connector) that
   *                              immediately sent the packet.
   * @param incomingPreparePacket A {@link InterledgerPreparePacket} containing data about an incoming payment.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   * will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  CompletableFuture<Optional<InterledgerResponsePacket>> handleIncomingData(
    AccountId accountId, InterledgerPreparePacket incomingPreparePacket
  );

}
