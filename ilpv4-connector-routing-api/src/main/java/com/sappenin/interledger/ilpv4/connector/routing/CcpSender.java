package com.sappenin.interledger.ilpv4.connector.routing;

import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlRequest;

/**
 * <p>Defines all operations necessary to send CCP messages to a single remote peer. Before routing updates are sent
 * to a remote peer, this sender must process a getRoute-control message to start processing. Then, once enabled, this
 * service will send routing updates on a pre-configured basis until it receives another route-control message
 * instructing it to stop broadcasting routes (or if the system administrator disables route-broadcasting).
 */
public interface CcpSender {

  /**
   * Handle an instance of {@link CcpRouteControlRequest} coming from a remote peer.
   *
   * @param routeControlRequest
   */
  void handleRouteControlRequest(CcpRouteControlRequest routeControlRequest);

  /**
   * Send a getRoute update to a remote peer.
   */
  void sendRouteUpdateRequest();

  /**
   * Stop this Sender from executing any further transmissions to its remote peer.
   */
  void stopBroadcasting();

}
