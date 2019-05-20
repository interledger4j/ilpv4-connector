package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import com.sappenin.interledger.ilpv4.connector.routing.ExternalRoutingService;
import com.sappenin.interledger.ilpv4.connector.routing.RoutableAccount;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
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
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.CCP_CONTROL_DESTINATION_ADDRESS;
import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.CCP_UPDATE_DESTINATION_ADDRESS;
import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION;


/**
 * An implementation of {@link PacketSwitchFilter} for all packets whose destination starts with the <tt>peer.</tt>
 * allocation scheme.
 */
public class PeerProtocolPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public static final InterledgerAddress PEER_DOT_ROUTE = InterledgerAddress.of("peer.route");

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final ExternalRoutingService externalRoutingService;
  private final AccountSettingsRepository accountSettingsRepository;
  private final CodecContext ccpCodecContext;
  private final CodecContext ildcpCodecContext;

  public PeerProtocolPacketFilter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PacketRejector packetRejector,
    final ExternalRoutingService externalRoutingService,
    final AccountSettingsRepository accountSettingsRepository,
    final CodecContext ccpCodecContext,
    final CodecContext ildcpCodecContext
  ) {
    super(packetRejector);
    this.connectorSettingsSupplier = connectorSettingsSupplier;
    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);

    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.ildcpCodecContext = Objects.requireNonNull(ildcpCodecContext);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    // All packets destined for `peer.config` should be handled here and returned....
    if (sourcePreparePacket.getDestination().startsWith(InterledgerAddress.AllocationScheme.PEER.getValue())) {

      // `peer.config`
      if (sourcePreparePacket.getDestination().equals(IldcpRequestPacket.PEER_DOT_CONFIG)) {
        if (connectorSettingsSupplier.get().getEnabledProtocols().isPeerConfigEnabled()) {
          return this.handleIldcpRequest(sourceAccountId, sourcePreparePacket);
        } else {
          return reject(sourceAccountId, sourcePreparePacket, InterledgerErrorCode.F00_BAD_REQUEST,
            "IL-DCP is not supported by this Connector.");
        }
      }

      // `peer.routing`
      else if (sourcePreparePacket.getDestination().startsWith(PEER_DOT_ROUTE)) {
        if (connectorSettingsSupplier.get().getEnabledProtocols().isPeerRoutingEnabled()) {
          //CCP via `peer.routing`
          return handlePeerRouting(sourceAccountId, sourcePreparePacket);
        } else {
          // Reject.
          return reject(sourceAccountId, sourcePreparePacket, InterledgerErrorCode.F00_BAD_REQUEST,
            "CCP routing protocol is not supported by this node.");
        }
      }

      // Unsupported `peer.` request...
      else {
        // Reject.
        return reject(
          sourceAccountId, sourcePreparePacket, InterledgerErrorCode.F01_INVALID_PACKET, "unknown peer protocol."
        );
      }
    }

    // Not a `peer.` request...
    else {
      return filterChain.doFilter(sourceAccountId, sourcePreparePacket);
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
    final AccountId sourceAccountId, final InterledgerPreparePacket ildcpRequestPacket
  ) throws InterledgerProtocolException {

    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(ildcpRequestPacket);

    return this.accountSettingsRepository.findByAccountId(sourceAccountId)
      .map(accountSettingsEntity -> {
        final InterledgerAddress childAddress = connectorSettingsSupplier.get().toChildAddress(sourceAccountId);

        // Convert IldcpResponse to bytes...
        final IldcpResponse ildcpResponse = IldcpResponse.builder()
          // TODO: Remove the short-cast once assetscale is converted to byte.
          .assetScale((short) accountSettingsEntity.getAssetScale())
          .assetCode(accountSettingsEntity.getAssetCode())
          .clientAddress(childAddress)
          .build();

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
          ildcpCodecContext.write(ildcpResponse, os);
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }

        return (InterledgerResponsePacket) InterledgerFulfillPacket.builder()
          .fulfillment(IldcpResponsePacket.EXECUTION_FULFILLMENT)
          .data(os.toByteArray())
          .build();
      })
      .orElseGet(() -> reject(sourceAccountId, ildcpRequestPacket, InterledgerErrorCode.F00_BAD_REQUEST,
        String.format("Invalid Source Account: `%s`", sourceAccountId.value()))
      );
  }

  @VisibleForTesting
  protected InterledgerResponsePacket handlePeerRouting(
    final AccountId sourceAccountId, InterledgerPreparePacket sourcePreparePacket
  ) {
    try {
      if (!sourcePreparePacket.getDestination().startsWith(PEER_DOT_ROUTE)) {
        return this.reject(
          sourceAccountId, sourcePreparePacket, InterledgerErrorCode.F02_UNREACHABLE,
          String.format("Unsupported Peer address: `%s`", PEER_DOT_ROUTE.getValue())
        );
      }

      if (!PEER_PROTOCOL_EXECUTION_CONDITION.equals(sourcePreparePacket.getExecutionCondition())) {
        return reject(
          sourceAccountId, sourcePreparePacket, InterledgerErrorCode.F01_INVALID_PACKET,
          "Packet does not contain correct condition for a peer protocol request."
        );
      }

      if (sourcePreparePacket.getDestination().equals(CCP_CONTROL_DESTINATION_ADDRESS)) {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
        final CcpRouteControlRequest routeControlRequest =
          ccpCodecContext.read(CcpRouteControlRequest.class, inputStream);

        final RoutableAccount routableAccount =
          this.externalRoutingService.getTrackedAccount(sourceAccountId).orElseThrow(
            () -> new RuntimeException(String.format("No tracked RoutableAccount found: `%s`", sourceAccountId.value()))
          );

        routableAccount.getCcpSender().handleRouteControlRequest(routeControlRequest);

        // Return the Ccp response...
        return InterledgerFulfillPacket.builder()
          .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
          .build();

      } else if (sourcePreparePacket.getDestination().equals(CCP_UPDATE_DESTINATION_ADDRESS)) {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourcePreparePacket.getData());
        final CcpRouteUpdateRequest routeUpdateRequest =
          ccpCodecContext.read(CcpRouteUpdateRequest.class, inputStream);

        final RoutableAccount routableAccount =
          this.externalRoutingService.getTrackedAccount(sourceAccountId)
            .orElseThrow(() -> new RuntimeException(
              String.format("No tracked RoutableAccount found (`%s`).", sourceAccountId))
            );

        routableAccount.getCcpReceiver().handleRouteUpdateRequest(routeUpdateRequest);

        // Return the Ccp response...
        return InterledgerFulfillPacket.builder()
          .fulfillment(CcpConstants.PEER_PROTOCOL_EXECUTION_FULFILLMENT)
          .build();

      } else {
        return reject(
          sourceAccountId, sourcePreparePacket,
          InterledgerErrorCode.F00_BAD_REQUEST,
          String.format("Unrecognized CCP message. destination=`%s`.", sourcePreparePacket.getDestination().getValue())
        );
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }


}
