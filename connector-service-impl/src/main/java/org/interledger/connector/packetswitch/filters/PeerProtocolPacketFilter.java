package org.interledger.connector.packetswitch.filters;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.connector.ccp.CcpConstants;
import org.interledger.connector.ccp.CcpRouteControlRequest;
import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.connector.routing.RoutableAccount;
import org.interledger.connector.routing.RouteBroadcaster;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import static org.interledger.connector.ccp.CcpConstants.CCP_CONTROL_DESTINATION_ADDRESS;
import static org.interledger.connector.ccp.CcpConstants.CCP_UPDATE_DESTINATION_ADDRESS;
import static org.interledger.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION;
import static org.interledger.connector.core.Ilpv4Constants.ALL_ZEROS_FULFILLMENT;

/**
 * An implementation of {@link PacketSwitchFilter} for all packets whose destination starts with the <tt>peer.</tt>
 * allocation scheme.
 */
public class PeerProtocolPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  @VisibleForTesting
  protected static final InterledgerAddress PEER_DOT_ROUTE = InterledgerAddress.of("peer.route");
  @VisibleForTesting
  protected static final InterledgerAddress PEER_DOT_SETTLE = InterledgerAddress.of("peer.settle");

  private static final boolean SENDING_NOT_ENABLED = false;
  private static final boolean RECEIVING_NOT_ENABLED = false;

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final RouteBroadcaster routeBroadcaster;
  private final CodecContext ccpCodecContext;
  private final CodecContext ildcpCodecContext;
  private final SettlementService settlementService;

  public PeerProtocolPacketFilter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PacketRejector packetRejector,
    final RouteBroadcaster routeBroadcaster,
    final CodecContext ccpCodecContext,
    final CodecContext ildcpCodecContext,
    final SettlementService settlementService
  ) {
    super(packetRejector);
    this.connectorSettingsSupplier = connectorSettingsSupplier;
    this.routeBroadcaster = Objects.requireNonNull(routeBroadcaster);
    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.ildcpCodecContext = Objects.requireNonNull(ildcpCodecContext);
    this.settlementService = Objects.requireNonNull(settlementService);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    // All packets destined for `peer.config` should be handled here and returned....
    if (sourcePreparePacket.getDestination().startsWith(InterledgerAddress.AllocationScheme.PEER.getValue())) {

      // `peer.config`
      if (sourcePreparePacket.getDestination().equals(IldcpRequestPacket.PEER_DOT_CONFIG)) {
        if (connectorSettingsSupplier.get().getEnabledProtocols().isPeerConfigEnabled()) {
          return this.handleIldcpRequest(sourceAccountSettings, sourcePreparePacket);
        } else {
          return packetRejector.reject(sourceAccountSettings.getAccountId(), sourcePreparePacket,
            InterledgerErrorCode.F00_BAD_REQUEST,
            "IL-DCP is not supported by this Connector.");
        }
      }

      // `peer.routing`
      else if (sourcePreparePacket.getDestination().startsWith(PEER_DOT_ROUTE)) {
        if (connectorSettingsSupplier.get().getEnabledProtocols().isPeerRoutingEnabled()) {
          //CCP via `peer.routing`
          return handlePeerRouting(sourceAccountSettings, sourcePreparePacket);
        } else {
          // Reject.
          return packetRejector.reject(sourceAccountSettings.getAccountId(), sourcePreparePacket,
            InterledgerErrorCode.F00_BAD_REQUEST,
            "CCP routing protocol is not supported by this node.");
        }
      }

      // `peer.settle` is a message coming from a Peer over ILPv4, destined for a Settlement Engine (connected to
      // this Connector) that correlates to the account link this packet was received on.
      else if (sourcePreparePacket.getDestination().startsWith(PEER_DOT_SETTLE)) {
        return handlePeerSettlement(sourceAccountSettings, sourcePreparePacket);
      }

      // Unsupported `peer.` request...
      else {
        // Reject.
        return packetRejector.reject(
          sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F01_INVALID_PACKET,
          "unknown peer protocol."
        );
      }
    }

    // Not a `peer.` request...
    else {
      return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
    }
  }

  /**
   * Request IL-DCP configuration information from a server.
   *
   * @param ildcpRequestPacket A {@link InterledgerPreparePacket} with all information necessary to construct a
   *                           response.
   *
   * @return An {@link IldcpResponsePacket} containing all information to allow an ILP node to act as a child of the
   * server.
   */
  @VisibleForTesting
  protected InterledgerResponsePacket handleIldcpRequest(
    final AccountSettings sourceAccountSettings, final InterledgerPreparePacket ildcpRequestPacket
  ) throws InterledgerProtocolException {

    Objects.requireNonNull(sourceAccountSettings);
    Objects.requireNonNull(ildcpRequestPacket);

    final InterledgerAddress childAddress =
      connectorSettingsSupplier.get().toChildAddress(sourceAccountSettings.getAccountId());

    // Convert IldcpResponse to bytes...
    final IldcpResponse ildcpResponse = IldcpResponse.builder()
      // TODO: Remove the short-cast once assetscale is converted to byte.
      .assetScale((short) sourceAccountSettings.getAssetScale())
      .assetCode(sourceAccountSettings.getAssetCode())
      .clientAddress(childAddress)
      .build();

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      ildcpCodecContext.write(ildcpResponse, os);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return InterledgerFulfillPacket.builder()
      .fulfillment(IldcpResponsePacket.EXECUTION_FULFILLMENT)
      .data(os.toByteArray())
      .build();
  }

  /**
   * The primary handler for all incoming CCP packets to support routing updates.
   */
  @VisibleForTesting
  protected InterledgerResponsePacket handlePeerRouting(
    final AccountSettings sourceAccountSettings, InterledgerPreparePacket sourcePreparePacket
  ) {
    try {
      if (!sourcePreparePacket.getDestination().startsWith(PEER_DOT_ROUTE)) {
        return packetRejector.reject(
          sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F02_UNREACHABLE,
          String.format("Unsupported Peer address. destinationAddress=%s", PEER_DOT_ROUTE.getValue())
        );
      }

      if (!PEER_PROTOCOL_EXECUTION_CONDITION.equals(sourcePreparePacket.getExecutionCondition())) {
        return packetRejector.reject(
          sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F01_INVALID_PACKET,
          "Packet does not contain correct condition for a peer protocol request."
        );
      }

      if (sourcePreparePacket.getDestination().equals(CCP_CONTROL_DESTINATION_ADDRESS)) {

        // If the account is not eligible for sending route updates, or the account doesn't exist, then
        // preemptively reject this request.
        final boolean preemptivelyReject = sourceAccountSettings.isSendRoutes() == SENDING_NOT_ENABLED;
        if (preemptivelyReject) {
          return packetRejector.reject(
            sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F00_BAD_REQUEST,
            String.format("CCP sending is not enabled for this account. destinationAddress=%s",
              sourcePreparePacket.getDestination().getValue())
          );
        }

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
        final CcpRouteControlRequest routeControlRequest =
          ccpCodecContext.read(CcpRouteControlRequest.class, inputStream);

        final RoutableAccount routableAccount =
          this.routeBroadcaster.getCcpEnabledAccount(sourceAccountSettings.getAccountId())
            .orElseThrow(() -> new RuntimeException(
              String.format("No tracked RoutableAccount found. accountId=%s", sourceAccountSettings.getAccountId()))
            );

        routableAccount.getCcpSender().handleRouteControlRequest(routeControlRequest);

        // Return the Ccp response...
        return InterledgerFulfillPacket.builder()
          .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
          .build();

      } else if (sourcePreparePacket.getDestination().equals(CCP_UPDATE_DESTINATION_ADDRESS)) {

        // If the account is not eligible for receiving route updates, or the account doesn't exist, then
        // preemptively reject this request.
        final boolean preemptiveReject = sourceAccountSettings.isReceiveRoutes() == RECEIVING_NOT_ENABLED;
        if (preemptiveReject) {
          return packetRejector.reject(
            sourceAccountSettings.getAccountId(), sourcePreparePacket, InterledgerErrorCode.F00_BAD_REQUEST,
            String.format("CCP receiving is not enabled for this account. destinationAddress=%s",
              sourcePreparePacket.getDestination().getValue())
          );
        }


        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
        final CcpRouteUpdateRequest routeUpdateRequest = ccpCodecContext.read(CcpRouteUpdateRequest.class, inputStream);

        final RoutableAccount routableAccount =
          this.routeBroadcaster.getCcpEnabledAccount(sourceAccountSettings.getAccountId())
            .orElseThrow(() -> new RuntimeException(
              String.format("No tracked RoutableAccount found accountId=%s", sourceAccountSettings.getAccountId()))
            );

        routableAccount.getCcpReceiver().handleRouteUpdateRequest(routeUpdateRequest);

        // Return the CCP response...
        return InterledgerFulfillPacket.builder()
          .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
          .build();

      } else {
        return packetRejector.reject(
          sourceAccountSettings.getAccountId(), sourcePreparePacket,
          InterledgerErrorCode.F00_BAD_REQUEST,
          String
            .format("Unrecognized CCP message. destinationAddress=%s", sourcePreparePacket.getDestination().getValue())
        );
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Handle an incoming ILP Prepare packet that contains peer-wise settlement engine messaging.
   *
   * @param sourcePreparePacketWithMessage A {@link InterledgerPreparePacket} with data sent from the peer's Settlement
   *                                       Engine.
   *
   * @return An {@link InterledgerResponsePacket} containing any response from this Connector's Settlement Engine.
   */
  @VisibleForTesting
  protected InterledgerResponsePacket handlePeerSettlement(
    final AccountSettings sourceAccountSettings, final InterledgerPreparePacket sourcePreparePacketWithMessage
  ) throws InterledgerProtocolException {
    Objects.requireNonNull(sourceAccountSettings);
    Objects.requireNonNull(sourcePreparePacketWithMessage);

    try {
      // NOTE: Idempotency is not required here because
      final byte[] messageFromOurSettlementEngine = this.settlementService.onSettlementMessageFromPeer(
        sourceAccountSettings, sourcePreparePacketWithMessage.getData()
      );

      return InterledgerFulfillPacket.builder()
        .fulfillment(ALL_ZEROS_FULFILLMENT)
        .data(messageFromOurSettlementEngine)
        .build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return packetRejector.reject(
        sourceAccountSettings.getAccountId(), sourcePreparePacketWithMessage,
        InterledgerErrorCode.T00_INTERNAL_ERROR,
        String.format("Error sending message to settlement engine. accountId=%s destinationAddress=%s",
          sourceAccountSettings.getAccountId(),
          sourcePreparePacketWithMessage.getDestination().getValue()
        ));
    }
  }
}
