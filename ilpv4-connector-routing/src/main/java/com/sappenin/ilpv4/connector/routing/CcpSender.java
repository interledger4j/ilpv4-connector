package com.sappenin.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.connector.ccp.*;
import com.sappenin.ilpv4.model.IlpRelationship;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import javax.annotation.PreDestroy;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

  /**
   * A default implementation of {@link CcpSender}.
   */
  class DefaultCcpSender implements CcpSender {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Supplier<ConnectorSettings> connectorSettingsSupplier;
    private final ForwardingRoutingTable<RouteUpdate> forwardingRoutingTable;
    private final AccountManager accountManager;
    private final CodecContext codecContext;

    // Note that the ILP Address and other settings for this sender are found in this Plugin.
    private final Plugin plugin;

    private final AtomicInteger lastKnownEpoch;
    private final AtomicReference<CcpSyncMode> syncMode;
    private final AtomicReference<UUID> lastKnownRoutingTableId;
    private final ConcurrentTaskScheduler scheduler;

    private final Object scheduledTaskLock = new Object();
    // This holds the scheduled getRoute update task. If nothing is scheduled, then this value will be null.
    private ScheduledFuture<?> scheduledTask;

    /**
     * Required-args Constructor.
     */
    public DefaultCcpSender(
      final Supplier<ConnectorSettings> connectorSettingsSupplier,
      final ForwardingRoutingTable<RouteUpdate> forwardingRoutingTable,
      final AccountManager accountManager,
      final CodecContext codecContext,
      final Plugin plugin
    ) {
      this.forwardingRoutingTable = Objects.requireNonNull(forwardingRoutingTable);
      this.accountManager = Objects.requireNonNull(accountManager);
      this.codecContext = codecContext;
      this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
      this.plugin = Objects.requireNonNull(plugin);

      this.syncMode = new AtomicReference<>(CcpSyncMode.MODE_IDLE);

      this.lastKnownEpoch = new AtomicInteger();
      this.lastKnownRoutingTableId = new AtomicReference<>(UUID.randomUUID());

      // Since a CcpSender only operates for a single peer, we only need a single thread.
      this.scheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
    }

    @Override
    public void handleRouteControlRequest(final CcpRouteControlRequest routeControlRequest) {
      Preconditions.checkNotNull(plugin, "Plugin must be assigned before using a CcpSender!");

      final InterledgerAddress peerAccountAddress = plugin.getPluginSettings().getPeerAccount();

      logger
        .debug("Peer `{}` sent CcpRouteControlRequest: {}", peerAccountAddress.getValue(),
          routeControlRequest);
      if (syncMode.get() != routeControlRequest.getMode()) {
        logger.debug(
          "Peer `{}` requested changing routing mode. oldMode={} newMode={}",
          peerAccountAddress.getValue(), this.syncMode.get(), routeControlRequest.getMode()
        );
      }
      this.syncMode.set(routeControlRequest.getMode());

      if (lastKnownRoutingTableId.get().equals(this.forwardingRoutingTable.getRoutingTableId()) == false) {
        logger.debug(
          "Peer `{}` has old routing table id, resetting lastKnownEpoch to 0. theirTableId=`{}` correctTableId=`{}`",
          peerAccountAddress.getValue(), lastKnownRoutingTableId,
          this.forwardingRoutingTable.getRoutingTableId());
        this.lastKnownEpoch.set(0);
      } else {
        logger
          .debug("Peer getEpoch set. getEpoch={} currentEpoch={}",
            peerAccountAddress.getValue(),
            lastKnownEpoch,
            this.forwardingRoutingTable.getCurrentEpoch());
        this.lastKnownEpoch.set(routeControlRequest.lastKnownEpoch());
      }

      /////////////////
      // We don't support any optional features, so we ignore the `features`
      /////////////////

      // Only startBroadcasting broadcasting if the sync-mode is MODE_SYNC.
      if (this.syncMode.get() == CcpSyncMode.MODE_SYNC) {
        // Start broadcasting routes to this peer, but only if another thread isn't already pending...
        this.startBroadcasting();
      } else {
        // Stop broadcasting routes to this peer.
        this.stopBroadcasting();
      }
    }

    public void startBroadcasting() {
      // Start broadcasting routes to the configured peer, but only if another thread isn't already pending...
      // Synchronize on the scheduledTask so that only a single thread at a time can enter this block...
      synchronized (scheduledTaskLock) {
        if (scheduledTask == null) {
          scheduledTask = this.scheduler.scheduleWithFixedDelay(
            this::sendRouteUpdateRequest,
            this.connectorSettingsSupplier.get().getRouteBroadcastSettings(
              plugin.getPluginSettings().getPeerAccount()
            ).getRouteBroadcastInterval()
          );
        } else {
          // Do nothing. The getRoute-update has already been scheduled...
        }
      }

    }

    @PreDestroy
    public void stopBroadcasting() {
      // Stop broadcasting routes to the configured peer.
      // Synchronize on the scheduledTask so that only a single thread at a time can enter this block...
      synchronized (scheduledTask) {
        try {
          if (this.scheduledTask != null) {
            this.scheduledTask.cancel(false);
          } else {
            // Do nothing. There is no scheduled task to cancel.
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
        this.scheduledTask = null;
      }
    }

    /**
     * Asynchronously send a getRoute update to a remote peer.
     */
    // @Async -- No need to manage this thread via Spring because this operation is manually scheduled in
    // response to getRoute control requests and other inputs from the routing service.
    public void sendRouteUpdateRequest() {
      Preconditions.checkNotNull(plugin, "Plugin must be assigned before using a CcpSender!");

      // Perform getRoute update, catch and log any exceptions...
      final InterledgerAddress peerAccountAddress = plugin.getPluginSettings().getPeerAccount();
      try {
        if (!this.plugin.isConnected()) {
          logger.warn("Cannot send routes, lpi2 not connected (yet). Plugin: {}", plugin);
          //return ImmutableRouteUpdateResults.builder().build();
          return;
        }

        //final Instant lastUpdate = Instant.now();
        final int nextRequestedEpoch = this.lastKnownEpoch.get();

        // Find all updates from the nextRequestedEpoch index to the (nextRequestedEpoch + maxPerTable) inside of the
        // routing table's Log. These are the udpates that should be sent to the remote peer.

        int skip = nextRequestedEpoch;
        int limit =
          (int) (nextRequestedEpoch + this.connectorSettingsSupplier.get().getRouteBroadcastSettings(peerAccountAddress)
            .getMaxEpochsPerRoutingTable());
        final Iterable<RouteUpdate> allUpdatesToSend = this.forwardingRoutingTable.getPartialRouteLog(skip, limit);

        // Despite asking for N updates, there may not be that many to send, so compute the `toEpoch` properly.
        final int toEpoch =
          nextRequestedEpoch + (int) StreamSupport.stream(allUpdatesToSend.spliterator(), false).count();

        // Filter the List....
        final List<RouteUpdate> filteredUpdatesToSend = StreamSupport.stream(allUpdatesToSend.spliterator(), false)
          .map(routeUpdate -> {

            // If there are no routes in the update, then skip it...
            if (!routeUpdate.getRoute().isPresent()) {
              return routeUpdate;
            } else {
              final Route actualRoute = routeUpdate.getRoute().get();
              // Don't send peer their own routes (i.e., withdraw this route)
              if (actualRoute.getNextHopAccount().equals(peerAccountAddress)) {
                return null;
              }

              // Don't advertise Peer or Supplier (Parent) routes to Suppliers (Parents).
              final boolean nextHopRelationIsPeerOrParent =
                this.accountManager.getAccountSettings(actualRoute.getNextHopAccount())
                  .map(accountSettings -> accountSettings.getRelationship() == IlpRelationship.PARENT ||
                    accountSettings.getRelationship() == IlpRelationship.PEER)
                  .orElseGet(() -> {
                    logger.error("NextHop Route {} was not found in the PeerManager!", actualRoute.getNextHopAccount());
                    return false;
                  });

              final boolean thisPluginIsParent =
                this.accountManager.getAccountSettings(peerAccountAddress)
                  .map(AccountSettings::getRelationship)
                  .map(relationship -> relationship == IlpRelationship.PARENT)
                  .orElse(false);

              if (thisPluginIsParent || nextHopRelationIsPeerOrParent) {
                // If the current plugin is our parent; OR, if th next-hop is a peer or Parent, then withdraw the
                // route. We only advertise routes to peers/children where the next-hop is a child.
                return null;
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
          .filter(routeUpdate -> routeUpdate.getRoute().isPresent())
          .map(RouteUpdate::getRoute)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(routingTableEntry ->
            ImmutableCcpNewRoute.builder()
              .prefix(routingTableEntry.getRoutePrefix())
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
          .filter(routeUpdate -> !routeUpdate.getRoute().isPresent())
          .map(routingTableEntry ->
            ImmutableCcpWithdrawnRoute.builder()
              .prefix(routingTableEntry.getRoutePrefix())
              .build()
          ).forEach(withdrawnRoutesBuilder::add);

        // Construct RouteUpdateRequest
        final CcpRouteUpdateRequest ccpRouteUpdateRequest = ImmutableCcpRouteUpdateRequest.builder()
          .speaker(this.connectorSettingsSupplier.get().getIlpAddress())
          .routingTableId(this.forwardingRoutingTable.getRoutingTableId())
          .holdDownTime(
            this.connectorSettingsSupplier.get().getRouteBroadcastSettings(peerAccountAddress).getRouteExpiry().toMillis()
          )
          .currentEpochIndex(this.forwardingRoutingTable.getCurrentEpoch())
          .fromEpochIndex(this.lastKnownEpoch.get())
          .toEpochIndex(toEpoch)
          .newRoutes(newRoutesBuilder.build())
          .withdrawnRoutePrefixes(withdrawnRoutesBuilder.build())
          .build();

        // Try to send the ccpRouteUpdateRequest....

        // We anticipate that they're going to be happy with our getRoute update and ccpRouteUpdateRequest the next one.
        final int previousNextRequestedEpoch = this.lastKnownEpoch.get();
        this.lastKnownEpoch.compareAndSet(previousNextRequestedEpoch, toEpoch);

        this.plugin.sendData(
          InterledgerPreparePacket.builder()
            .amount(BigInteger.ZERO)
            .destination(CcpConstants.CCP_UPDATE_DESTINATION_ADDRESS)
            .executionCondition(CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION)
            .expiresAt(Instant.now().plus(
              this.connectorSettingsSupplier.get().getRouteBroadcastSettings(peerAccountAddress).getRouteExpiry()
            ))
            .data(serializeCcpPacket(ccpRouteUpdateRequest))
            .build()
        ).handle((fulfillment, error) -> {
          if (error != null) {
            logger.error("Failed to broadcast getRoute information to peer. peer=`{}`: {}",
              peerAccountAddress.getValue(), error);
          } else {
            logger
              .debug("Route update succeeded to peer: `{}`!", peerAccountAddress.getValue());
          }
          return null;
        });

        //return ImmutableRouteUpdateResults.builder().build();
      } catch (IOException | RuntimeException e) {
        logger
          .error("Failed to broadcast getRoute information to peer. peer=`{}`",
            peerAccountAddress.getValue(), e);
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

    public CcpSyncMode getSyncMode() {
      return syncMode.get();
    }

    @VisibleForTesting
    protected ForwardingRoutingTable<RouteUpdate> getForwardingRoutingTable() {
      return this.forwardingRoutingTable;
    }
  }
}
