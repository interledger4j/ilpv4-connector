package com.sappenin.interledger.ilpv4.connector.plugins.peer;

import com.sappenin.interledger.ilpv4.connector.plugins.InternallyRoutedPlugin;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A LPIv2 {@link Plugin} that implements the ILP Peer Config sub-protocol, which allows two peers to exchange
 * routing information.</p>
 *
 * <p>This plugin functions like the PeerController in the Javascript implementation.</p>
 */
public class PeerRouteProtocolPlugin extends InternallyRoutedPlugin implements Plugin<PluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "PEER_ROUTE_PLUGIN";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);

  public static final InterledgerAddress PEER_DOT_ROUTE = InterledgerAddress.of("peer.route");
  public static final InterledgerAddressPrefix PEER_DOT_ROUTE_PREFIX = InterledgerAddressPrefix.from(PEER_DOT_ROUTE);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ExternalRoutingService externalRoutingService;

  /**
   * Required-args Constructor.
   *
   * @param pluginSettings  A {@link PluginSettings} that specifies all plugin options.
   * @param oerCodecContext
   */
  public PeerRouteProtocolPlugin(
    final PluginSettings pluginSettings, final CodecContext oerCodecContext, final ExternalRoutingService externalRoutingService
  ) {
    super(pluginSettings, oerCodecContext);

    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> sendData(InterledgerPreparePacket preparePacket) {
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
//              .triggeredBy(this.getPluginSettings().getLocalNodeAddress())
//              .code(InterledgerErrorCode.F02_UNREACHABLE)
//              .build()
//          );
//        }
//
//        if (!PEER_PROTOCOL_EXECUTION_CONDITION.equals(preparePacket.getExecutionCondition())) {
//          return Optional.of(
//            InterledgerRejectPacket.builder()
//              .message("Packet does not contain correct condition for a peer protocol request.")
//              .triggeredBy(this.getPluginSettings().getLocalNodeAddress())
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
//            this.externalRoutingService.getTrackedAccount(this.getPluginSettings().getAccountAddress())
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
//            this.externalRoutingService.getTrackedAccount(this.getPluginSettings().getAccountAddress())
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
//              .triggeredBy(this.getPluginSettings().getLocalNodeAddress())
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
