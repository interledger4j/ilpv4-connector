package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>A {@link PacketSwitchFilter} that implements the ILP Peer Route sub-protocol, which allows two peers to exchange
 * routing information.</p>
 *
 * <p>Note: This link functions like the PeerController in the Javascript implementation.</p>
 */
public class PeerRouteFilter implements PacketSwitchFilter {

  public static final InterledgerAddress PEER_DOT_ROUTE = InterledgerAddress.of("peer.route");
  public static final InterledgerAddressPrefix PEER_DOT_ROUTE_PREFIX = InterledgerAddressPrefix.from(PEER_DOT_ROUTE);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<InterledgerAddress> nodeOperatorAddressSupplier;

  /**
   * Required-args Constructor.
   */
  public PeerRouteFilter(final Supplier<InterledgerAddress> nodeOperatorAddressSupplier) {
    this.nodeOperatorAddressSupplier = Objects.requireNonNull(nodeOperatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);
    Objects.requireNonNull(filterChain);

    // TODO - FIXME!
    return filterChain.doFilter(sourceAccountId, sourcePreparePacket);
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
