package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.connector.ccp.CcpRouteControlRequest;

/**
 * <p>Defines all operations necessary to send CCP messages to a single remote peer. Before routing updates are sent
 * to a remote peer, this sender must process a getRoute-control message to startBroadcasting processing. Then, once
 * enabled, this service will send routing updates on a pre-configured basis until it receives another getRoute-control
 * message instructing it to stopBroadcasting broadcasting routes (or if the system administrator disables
 * getRoute-broadcasting).
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
   * Stop this Sender from executing any furhter transmissions to its remote peer.
   */
  void stopBroadcasting();

}
