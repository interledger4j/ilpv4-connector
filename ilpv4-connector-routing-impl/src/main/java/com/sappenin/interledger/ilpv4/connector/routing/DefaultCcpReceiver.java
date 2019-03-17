package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteControlRequest;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRoutePathPart;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpRouteUpdateRequest;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpSyncMode;
import com.sappenin.interledger.ilpv4.connector.ccp.CcpWithdrawnRoute;
import com.sappenin.interledger.ilpv4.connector.ccp.ImmutableCcpRouteControlRequest;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Default implementation of {@link CcpReceiver}.
 */
public class DefaultCcpReceiver implements CcpReceiver {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ForwardingRoutingTable<IncomingRoute> incomingRoutes;
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final CodecContext ccpCodecContext;

  // The Plugin that can communicate to the remote peer.
  private final AccountId peerAccountId;
  private final Link link;

  // Contains the identifier used used by our peer. We'll reset the getEpoch to 0 if the identifier changes.
  private Instant routingTableExpiry = Instant.EPOCH;

  public DefaultCcpReceiver(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountId peerAccountId, final Link link, final ForwardingRoutingTable<IncomingRoute> incomingRoutes,
    final CodecContext ccpCodecContext
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.incomingRoutes = Objects.requireNonNull(incomingRoutes);
    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.peerAccountId = peerAccountId;
    this.link = Objects.requireNonNull(link);
  }

  @Override
  public List<InterledgerAddressPrefix> handleRouteUpdateRequest(
    final CcpRouteUpdateRequest routeUpdateRequest
  ) {
    Objects.requireNonNull(routeUpdateRequest);

    // Extend the routingTableExpiry of the routing-table held by this receiver...
    this.bump(routeUpdateRequest.holdDownTime());

    // If the remote peer/account has a new routing table Id, then we reset the getEpoch so that we can update all
    // routes from the remote.
    if (!this.incomingRoutes.getRoutingTableId().equals(routeUpdateRequest.routingTableId())) {
      logger.debug("Saw new routing table. oldId={} newId={}", this.incomingRoutes.getRoutingTableId(),
        routeUpdateRequest.routingTableId());
      this.incomingRoutes
        .compareAndSetRoutingTableId(incomingRoutes.getRoutingTableId(), routeUpdateRequest.routingTableId());
      this.incomingRoutes.compareAndSetCurrentEpoch(incomingRoutes.getCurrentEpoch(), 0);
    }

    // If this happens, then the entire routing table will expire, and we'll startBroadcasting over eventually...?
    if (routeUpdateRequest.fromEpochIndex() > this.incomingRoutes.getCurrentEpoch()) {
      // There is a gap, we need to go back to the last getEpoch we have
      logger.debug("Gap in routing updates. expectedEpoch={} actualFromEpoch={}",
        this.incomingRoutes.getCurrentEpoch(), routeUpdateRequest.fromEpochIndex()
      );
      return Collections.emptyList();
    }

    if (this.incomingRoutes.getCurrentEpoch() > routeUpdateRequest.toEpochIndex()) {
      // This routing update is older than what we already have
      logger.debug("Old routing update, ignoring. expectedEpoch={} actualToEpoch={}",
        this.incomingRoutes.getCurrentEpoch(), routeUpdateRequest.toEpochIndex()
      );
      return Collections.emptyList();
    }

    // just a heartbeat
    if (routeUpdateRequest.newRoutes().size() == 0 && routeUpdateRequest.withdrawnRoutePrefixes().size() == 0) {
      logger.debug("Pure heartbeat. fromEpoch={} toEpoch={}",
        routeUpdateRequest.fromEpochIndex(), routeUpdateRequest.toEpochIndex()
      );
      this.incomingRoutes
        .compareAndSetCurrentEpoch(incomingRoutes.getCurrentEpoch(), routeUpdateRequest.toEpochIndex());
      return Collections.emptyList();
    }


    final ImmutableList.Builder<InterledgerAddressPrefix> changedPrefixesBuilder = ImmutableList.builder();

    // Withdrawn Routes...
    if (routeUpdateRequest.withdrawnRoutePrefixes().size() > 0) {
      logger.debug("Informed of no-longer-reachable routes. count={} routes={}",
        routeUpdateRequest.withdrawnRoutePrefixes().size(), routeUpdateRequest.withdrawnRoutePrefixes()
      );

      routeUpdateRequest.withdrawnRoutePrefixes().stream()
        .map(CcpWithdrawnRoute::prefix)
        .forEach(withdrawnRoutePrefix -> {
          this.incomingRoutes.removeRoute(withdrawnRoutePrefix);
          changedPrefixesBuilder.add(withdrawnRoutePrefix);
        });
    }

    // New Routes
    routeUpdateRequest.newRoutes().stream()
      .map(ccpNewRoute -> ImmutableIncomingRoute.builder()
        .peerAccountId(peerAccountId)
        .routePrefix(ccpNewRoute.prefix())
        .path(
          ccpNewRoute.path().stream()
            .map(CcpRoutePathPart::routePathPart)
            .collect(Collectors.toList())
        )
        .auth(ccpNewRoute.auth())
        .build()
      )
      .forEach(newIncomingRoute -> {
        if (this.incomingRoutes.addRoute(newIncomingRoute.getRoutePrefix(), newIncomingRoute)) {
          changedPrefixesBuilder.add(newIncomingRoute.getRoutePrefix());
        }
      });

    this.incomingRoutes
      .compareAndSetCurrentEpoch(this.incomingRoutes.getCurrentEpoch(), routeUpdateRequest.toEpochIndex());

    final List<InterledgerAddressPrefix> changedPrefixes = changedPrefixesBuilder.build();
    logger.debug("Applied getRoute update. changedPrefixesCount={} fromEpoch={} toEpoch={}",
      changedPrefixes.size(), routeUpdateRequest.fromEpochIndex(), routeUpdateRequest.toEpochIndex()
    );
    return changedPrefixes;
  }

  public InterledgerResponsePacket sendRouteControl() {
    Preconditions.checkNotNull(link, "Plugin must be assigned before using a CcpReceiver!");

    final ImmutableCcpRouteControlRequest request = ImmutableCcpRouteControlRequest.builder()
      .mode(CcpSyncMode.MODE_SYNC)
      .lastKnownRoutingTableId(this.incomingRoutes.getRoutingTableId())
      .lastKnownEpoch(this.incomingRoutes.getCurrentEpoch())
      .build();

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .amount(BigInteger.ZERO)
      .destination(CcpConstants.CCP_CONTROL_DESTINATION_ADDRESS)
      .executionCondition(CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION)
      .expiresAt(Instant.now().plus(connectorSettingsSupplier.get().getGlobalRoutingSettings().getRouteExpiry()))
      .data(serializeCcpPacket(request))
      .build();

    // Link handles retry, if any...
    try {
      InterledgerResponsePacket responsePacket = this.link.sendPacket(preparePacket);
      logger.debug("Successfully sent getRoute control message. peer={}", peerAccountId);
      return responsePacket;
    } catch (Exception e) {
      if (e instanceof InterledgerProtocolException) {
        final InterledgerRejectPacket rejectPacket =
          ((InterledgerProtocolException) e).getInterledgerRejectPacket();
        logger.debug("Route control message was rejected. rejection={}", rejectPacket.getMessage());
        return rejectPacket;
      } else {
        throw new RuntimeException(String.format("Unknown response fulfillPacket type. peer=`%s`; error=`%s`;",
          peerAccountId, e.getMessage()), e);
      }
    }
  }

  @Override
  public void forEachIncomingRoute(final Consumer<IncomingRoute> action) {
    this.incomingRoutes.forEach(action);
  }

  @Override
  public Iterable<IncomingRoute> getAllIncomingRoutes() {
    return this.incomingRoutes.getAllRoutes();
  }

  @Override
  public Optional<IncomingRoute> getRouteForPrefix(InterledgerAddressPrefix addressPrefix) {
    return StreamSupport.stream(this.getAllIncomingRoutes().spliterator(), false)
      // TODO: Will go away once routes are 1-1.
      .filter(incomingRoute -> incomingRoute.getRoutePrefix().equals(addressPrefix))
      .findFirst();
  }

  /**
   * Bump up the routingTableExpiry of this routing table by the number of milliseconds indicated in {@code
   * holdDownTimeMillis}.
   *
   * @param holdDownTimeMillis the number of millis to extend the expiration of the current routing table.
   */
  protected void bump(final long holdDownTimeMillis) {

    final Instant requestedExpiry = Instant.now().plusMillis(holdDownTimeMillis);

    if (this.routingTableExpiry.isBefore(requestedExpiry)) {
      this.routingTableExpiry = requestedExpiry;
    }
  }

  @VisibleForTesting
  protected byte[] serializeCcpPacket(final CcpRouteControlRequest ccpRouteControlRequest) {
    Objects.requireNonNull(ccpRouteControlRequest);

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ccpCodecContext.write(ccpRouteControlRequest, outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  protected Instant getRoutingTableExpiry() {
    return this.routingTableExpiry;
  }
}
