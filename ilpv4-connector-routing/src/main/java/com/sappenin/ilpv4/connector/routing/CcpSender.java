package com.sappenin.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.connector.ccp.*;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.Plugin;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.model.settings.RouteBroadcastSettings;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import javax.inject.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * <p>Defines all operations necessary to send CCP messages to a single remote peer. Before routing updates are sent
 * to a remote peer, this sender must process a route-control message to start processing. Then, once enabled, this
 * service will send routing updates on a pre-configured basis until it receives another route-control message
 * instructing it to stop broadcasting routes (or if the system administrator disables route-broadcasting).
 */
public interface CcpSender {

  /**
   * Handle an instance of {@link CcpRouteControlRequest} coming from a remote peer.
   *
   * @param routeControlRequest
   */
  void onRouteControlRequest(final CcpRouteControlRequest routeControlRequest);

  /**
   * A default implementation of {@link CcpSender}.
   */
  class DefaultCcpSender implements CcpSender {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RoutingTable<RoutingTableEntry> routingTable;

    private final Plugin plugin;
    private final AccountManager accountManager;
    private final CodecContext codecContext;

    private final AtomicLong lastKnownEpoch;
    private final AtomicReference<CcpSyncMode> syncMode;
    private final AtomicReference<UUID> lastKnownRoutingTableId;

    // This must be computed before this class is constructed...
    private final Provider<RouteBroadcastSettings> routeBroadcastSettingsProvider;
    private final Provider<ConnectorSettings> connectorSettingsProvider;

    private final ConcurrentTaskScheduler scheduler;

    // This is the scheduled route update. If nothing is scheduled, then this value will be null.
    private ScheduledFuture<?> scheduledTask;

    /**
     * Required-args Constructor.
     */
    public DefaultCcpSender(
      final RoutingTable<RoutingTableEntry> routingTable, final AccountManager accountManager,
      final Provider<RouteBroadcastSettings> routeBroadcastSettingsProvider,
      final Plugin plugin, CodecContext codecContext, final Provider<ConnectorSettings> connectorSettingsProvider
    ) {
      this.routingTable = Objects.requireNonNull(routingTable);
      this.accountManager = Objects.requireNonNull(accountManager);
      this.routeBroadcastSettingsProvider = Objects.requireNonNull(routeBroadcastSettingsProvider);
      this.codecContext = codecContext;
      this.connectorSettingsProvider = Objects.requireNonNull(connectorSettingsProvider);
      this.plugin = Objects.requireNonNull(plugin);

      this.syncMode = new AtomicReference<>(CcpSyncMode.MODE_IDLE);

      this.lastKnownEpoch = new AtomicLong();
      this.lastKnownRoutingTableId = new AtomicReference<>(UUID.randomUUID());

      // Since a CcpSender only operates for a single peer, we only need a single thread.
      this.scheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
    }

    //    TODO: Simple route control would be -- if we're in the right state, and we're not sending, and we didnt just send,
    //    then schedule now. Catch any exceptions and set isRunning to false, with a proper timeout and we should be good.
    //    Check the plugin timeout.
    @Override
    public void onRouteControlRequest(final CcpRouteControlRequest routeControlRequest) {
      logger.trace("Peer sent CcpRouteControlRequest: {}", routeControlRequest);
      if (syncMode.get() != routeControlRequest.getMode()) {
        logger.trace(
          "Peer requested changing routing mode. oldMode={} newMode={}",
          this.syncMode.get(), routeControlRequest.getMode()
        );
      }
      this.syncMode.set(routeControlRequest.getMode());

      if (lastKnownRoutingTableId.get().equals(this.routingTable.getRoutingTableId()) == false) {
        logger.trace("Peer has old routing table id, resetting lastKnownEpoch to 0. theirTableId={} correctTableId={}",
          lastKnownRoutingTableId, this.routingTable.getRoutingTableId());
        this.lastKnownEpoch.set(0);
      } else {
        logger
          .trace("Peer epoch set. epoch={} currentEpoch={}", this.plugin.getAccountAddress(), lastKnownEpoch,
            this.routingTable.getCurrentEpoch());
        this.lastKnownEpoch.set(routeControlRequest.lastKnownEpoch());
      }

      /////////////////
      // We don't support any optional features, so we ignore the `features`
      /////////////////

      // Only start broadcasting if the sync-mode is MODE_SYNC.
      if (this.syncMode.get() == CcpSyncMode.MODE_SYNC) {
        // Start broadcasting routes to this peer, but only if another thread isn't already pending...
        // Synchronize on the scheduledTask so that only a single thread at a time can enter this block...
        synchronized (scheduledTask) {
          if (scheduledTask == null) {
            scheduledTask = this.scheduler.scheduleWithFixedDelay(
              this::sendRouteUpdateRequest, this.routeBroadcastSettingsProvider.get().routeBroadcastInterval()
            );
          } else {
            // Do nothing. The route-update has already been scheduled...
          }
        }
      } else {
        // Stop broadcasting routes to this peer
        synchronized (scheduledTask) {
          if (scheduledTask != null) {
            // Let the in-process task, if any, to complete before unscheduling the Runnable.
            scheduledTask.cancel(false);
          } else {
            // Do nothing. There is no scheduled task to cancel.
          }
        }
      }
    }

    /**
     * Asynchronously send a route update to a remote peer.
     */
    @Async
    public void sendRouteUpdateRequest() {
      // Perform route update, catch and log any exceptions...
      try {
        if (!this.plugin.isConnected()) {
          logger.warn("Cannot send routes, plugin not connected (yet). Plugin: {}", plugin);
          //return ImmutableRouteUpdateResults.builder().build();
          return;
        }

        final Instant lastUpdate = Instant.now();
        final long nextRequestedEpoch = this.lastKnownEpoch.get();

        // Find all updates from the nextRequestedEpoch index to the (nextRequestedEpoch + maxPerTable) inside of the
        // routing table's Log. These are the udpates that should be sent to the remote peer.
        final List<RouteUpdate> allUpdatesToSend = this.routingTable.getRouteUpdateLog().subList(
          (int) nextRequestedEpoch,
          (int) (nextRequestedEpoch + routeBroadcastSettingsProvider.get().maxEpochsPerRoutingTable())
        );

        // Despite asking for N updates, there may not be that many to send, so compute the `toEpoch` properly.
        final long toEpoch = nextRequestedEpoch + allUpdatesToSend.size();

        // Filter the List....
        final List<RouteUpdate> filteredUpdatesToSend = allUpdatesToSend.stream()
          .map(routeUpdate -> {

            // If there are no routes in the update, then skip it...
            if (!routeUpdate.route().isPresent()) {
              return routeUpdate;
            } else {
              final RoutingTableEntry actualRoute = routeUpdate.route().get();
              // Don't send peer their own routes
              if (actualRoute.getNextHopAccount().equals(this.plugin.getAccountAddress())) {
                return routeUpdate.withoutRoutes();
              }

              // Don't advertise Peer or Provider (Parent) routes to Providers (Parents).
              final boolean nextHopRelationIsPeerOrParent =
                this.accountManager.getAccountSettings(actualRoute.getNextHopAccount())
                  .map(accountSettings -> accountSettings.getRelationship() == IlpRelationship.PARENT ||
                    accountSettings.getRelationship() == IlpRelationship.PEER)
                  .orElseGet(() -> {
                    logger.error("NextHop Route {} was not found in the PeerManager!", actualRoute.getNextHopAccount());
                    return false;
                  });

              final boolean thisPluginIsParent = this.accountManager.getAccountSettings(this.plugin.getAccountAddress())
                .map(AccountSettings::getRelationship)
                .map(relationship -> relationship == IlpRelationship.PARENT)
                .orElse(false);

              if (thisPluginIsParent || nextHopRelationIsPeerOrParent) {
                // If the current plugin is our parent; OR, if th next-hop is a peer or Parent, then withdraw the route.
                // We only adverise routes to peers/children where the next-hop is a child.
                return routeUpdate.withoutRoutes();
              } else {
                return routeUpdate;
              }
            }
          })
          .collect(Collectors.toList());

        final ImmutableList.Builder<CcpNewRoute> newRoutesBuilder = ImmutableList.builder();
        final ImmutableList.Builder<CcpWithdrawnRoute> withdrawnRoutesBuilder = ImmutableList.builder();

        // Populate newRoutesBuilder....
        filteredUpdatesToSend.stream()
          .filter(routeUpdate -> routeUpdate.route().isPresent())
          .map(RouteUpdate::route)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(routingTableEntry ->
            ImmutableCcpNewRoute.builder()
              .prefix(routingTableEntry.getTargetPrefix())
              .auth(routingTableEntry.getAuth())
              .path(
                routingTableEntry.getPath().stream()
                  .map(address -> ImmutableCcpRoutePathPart.builder()
                    .routePathPart(address)
                    .build()
                  )
                  .collect(Collectors.toList())
              )
              .build()
          ).forEach(newRoutesBuilder::add);

        // Populate withdrawnRoutesBuilder....
        filteredUpdatesToSend.stream()
          .filter(routeUpdate -> !routeUpdate.route().isPresent())
          .map(routingTableEntry ->
            ImmutableCcpWithdrawnRoute.builder()
              .prefix(routingTableEntry.prefix())
              .build()
          ).forEach(withdrawnRoutesBuilder::add);

        // Construct RouteUpdateRequest
        final CcpRouteUpdateRequest ccpRouteUpdateRequest = ImmutableCcpRouteUpdateRequest.builder()
          .speaker(this.connectorSettingsProvider.get().getIlpAddress())
          .routingTableId(this.routingTable.getRoutingTableId())
          .holdDownTime(this.routeBroadcastSettingsProvider.get().routeExpiry().toMillis())
          .currentEpochIndex(this.routingTable.getCurrentEpoch())
          .fromEpochIndex(this.lastKnownEpoch.get())
          .toEpochIndex(toEpoch)
          .newRoutes(newRoutesBuilder.build())
          .withdrawnRoutePrefixes(withdrawnRoutesBuilder.build())
          .build();

        // Try to send the ccpRouteUpdateRequest....

        // We anticipate that they're going to be happy with our route update and ccpRouteUpdateRequest the next one.
        final long previousNextRequestedEpoch = this.lastKnownEpoch.get();
        this.lastKnownEpoch.compareAndSet(previousNextRequestedEpoch, toEpoch);

        this.plugin.sendPacket(
          InterledgerPreparePacket.builder()
            .amount(BigInteger.ZERO)
            .destination(CcpConstants.CCP_UPDATE_DESTINATION)
            .executionCondition(CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION)
            .expiresAt(Instant.now().plus(this.routeBroadcastSettingsProvider.get().routeExpiry()))
            .data(serializeCcpPacket(ccpRouteUpdateRequest))
            .build()
        ).handle((fulfillment, error) -> {
          if (error != null) {
            logger
              .error("Failed to broadcast route information to peer. peer={}", plugin.getAccountAddress(), error);
          } else {
            logger.trace("Route update succeeded!");
          }
          return null;
        });

        //return ImmutableRouteUpdateResults.builder().build();
      } catch (IOException | RuntimeException e) {
        logger
          .error("Failed to broadcast route information to peer. peer={}", plugin.getAccountAddress(), e);
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
      }
    }

    @VisibleForTesting
    protected byte[] serializeCcpPacket(final CcpRouteUpdateRequest ccpRouteUpdateRequest) throws IOException {
      Objects.requireNonNull(ccpRouteUpdateRequest);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      codecContext.write(ccpRouteUpdateRequest, outputStream);
      return outputStream.toByteArray();
    }

    //    /**
    //     * The result of a remote routing update.
    //     */
    //    @Value.Immutable
    //    interface RouteUpdateResults {
    //
    //    }
  }
}
