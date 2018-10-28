package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.plugin.lpiv2.Plugin;

import java.util.Optional;

/**
 * Defines a centralized service that manages all routing decisions for an ILPv4 Connector.
 */
public interface RoutingService {

  /**
   * Register this service to respond to connect/disconnect events that may be emitted from a {@link Plugin}, and then
   * add the account to this service's internal machinery.
   *
   * @param peerAccount The address of a remote peer that can have packets routed to it.
   */
  void trackAccount(InterledgerAddress peerAccount);

  /**
   * Untrack a peer account address from participating in Routing.
   *
   * @param peerAccount The address of a remote peer that can have packets routed to it.
   */
  void untrackAccount(InterledgerAddress peerAccount);

  void setPeer(InterledgerAddress peerAccountAddress, Peer peer);

  Optional<Peer> getPeer(InterledgerAddress peerAccountAddress);

}
