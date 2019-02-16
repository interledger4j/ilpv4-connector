package com.sappenin.interledger.ilpv4.connector.links.peer;

import com.sappenin.interledger.ilpv4.connector.links.InternallyRoutedLink;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>A LPIv2 {@link Link} that implements the ILP Peer Config sub-protocol, which allows two peers to exchange
 * routing information.</p>
 *
 * <p>This link functions like the PeerController in the Javascript implementation.</p>
 */
public class PeerRouteProtocolLink extends InternallyRoutedLink implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "PEER_ROUTE_LINK";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerAddress PEER_DOT_ROUTE = InterledgerAddress.of("peer.route");
  public static final InterledgerAddressPrefix PEER_DOT_ROUTE_PREFIX = InterledgerAddressPrefix.from(PEER_DOT_ROUTE);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ExternalRoutingService externalRoutingService;

  /**
   * Required-args Constructor.
   *
   * @param linkSettings    A {@link LinkSettings} that specifies all link options.
   * @param oerCodecContext
   */
  public PeerRouteProtocolLink(
    final LinkSettings linkSettings, final CodecContext oerCodecContext, final ExternalRoutingService externalRoutingService
  ) {
    super(linkSettings, oerCodecContext);

    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
  }

  @Override
  public Optional<InterledgerResponsePacket> sendPacket(InterledgerPreparePacket preparePacket) {
    // TODO
    throw new RuntimeException("FIXME!");
  }

  //  @Override
  //  public CompletableFuture<Optional<InterledgerResponsePacket>> doSendData(InterledgerPreparePacket preparePacket) {
  //
  //    return CompletableFuture.supplyAsync(() -> {
  //      try {
  //        if (preparePacket.getDestination().startsWith(PEER_DOT_ROUTE)) {
  //          return Optional.of(
  //            InterledgerRejectPacket.builder()
  //              .message(String.format("Unsupported Peer address: `%s`", PEER_DOT_ROUTE.getValue()))
  //              .triggeredBy(this.getLinkSettings().getLocalNodeAddress())
  //              .code(InterledgerErrorCode.F02_UNREACHABLE)
  //              .build()
  //          );
  //        }
  //
  //        if (!PEER_PROTOCOL_EXECUTION_CONDITION.equals(preparePacket.getExecutionCondition())) {
  //          return Optional.of(
  //            InterledgerRejectPacket.builder()
  //              .message("Packet does not contain correct condition for a peer protocol request.")
  //              .triggeredBy(this.getLinkSettings().getLocalNodeAddress())
  //              .code(InterledgerErrorCode.F01_INVALID_PACKET)
  //              .build()
  //          );
  //        }
  //
  //        if (preparePacket.getDestination().equals(CCP_CONTROL_DESTINATION_ADDRESS)) {
  //          final ByteArrayInputStream inputStream = new ByteArrayInputStream(preparePacket.getData());
  //          final CcpRouteControlRequest routeControlRequest =
  //            getIlpCodecContext().read(CcpRouteControlRequest.class, inputStream);
  //
  //          final RoutableAccount routableAccount =
  //            this.externalRoutingService.getTrackedAccount(this.getLinkSettings().getAccountAddress())
  //              .orElseThrow(() -> new RuntimeException("No tracked RoutableAccount found!"));
  //
  //          routableAccount.getCcpSender().handleRouteControlRequest(routeControlRequest);
  //
  //          // Return the Ccp response...
  //          return Optional.of(InterledgerFulfillPacket.builder()
  //            .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
  //            .build());
  //
  //        } else if (preparePacket.getDestination().equals(CCP_UPDATE_DESTINATION_ADDRESS)) {
  //          final ByteArrayInputStream inputStream = new ByteArrayInputStream(preparePacket.getData());
  //          final CcpRouteUpdateRequest routeUpdateRequest =
  //            getIlpCodecContext().read(CcpRouteUpdateRequest.class, inputStream);
  //
  //          final RoutableAccount routableAccount =
  //            this.externalRoutingService.getTrackedAccount(this.getLinkSettings().getAccountAddress())
  //              .orElseThrow(() -> new RuntimeException("No tracked RoutableAccount found!"));
  //
  //          routableAccount.getCcpReceiver().handleRouteUpdateRequest(routeUpdateRequest);
  //
  //          // Return the Ccp response...
  //          return Optional.of(
  //            InterledgerFulfillPacket.builder()
  //              .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
  //              .build()
  //          );
  //        } else {
  //          return Optional.of(
  //            InterledgerRejectPacket.builder()
  //              .message(
  //                String.format("Unrecognized CCP message. destination=`%s`.", preparePacket.getDestination().getValue())
  //              )
  //              .triggeredBy(this.getLinkSettings().getLocalNodeAddress())
  //              .code(InterledgerErrorCode.F00_BAD_REQUEST)
  //              .build()
  //          );
  //        }
  //      } catch (IOException e) {
  //        logger.error(e.getMessage(), e);
  //        return Optional.empty();
  //      }
  //
  //    });

  // }

}
