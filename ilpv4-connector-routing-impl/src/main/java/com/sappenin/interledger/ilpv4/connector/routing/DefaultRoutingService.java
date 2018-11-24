package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.model.StaticRoute;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.events.PluginConnectedEvent;
import org.interledger.plugin.lpiv2.events.PluginDisconnectedEvent;
import org.interledger.plugin.lpiv2.events.PluginErrorEvent;
import org.interledger.plugin.lpiv2.events.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A default implementation of {@link RoutingService}.
 */
public class DefaultRoutingService implements RoutingService {

  private static final boolean SHOULD_SEND_ROUTES = true;
  private static final boolean SHOULD_NOT_SEND_ROUTES = false;
  private static final boolean SHOULD_RECEIVE_ROUTES = true;
  private static final boolean SHOULD_NOT_RECEIVE_ROUTES = false;
  private static final boolean ROUTES_HAVE_CHANGED = true;
  private static final boolean ROUTES_HAVE_NOT_CHANGED = false;
  private static final String HMAC_SHA_256 = "HmacSHA256";
  private static final String UTF_8 = "UTF-8";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final UUID routingServiceListenerId;

  private final CodecContext codecContext;

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  // Local routing table, used for actually routing packets.
  private final RoutingTable<Route> localRoutingTable;

  // Used for routes that this connector has received from peer accounts.
  private final ForwardingRoutingTable<IncomingRoute> incomingRoutingTable;

  // Master outgoing routing table, used for routes that this connector broadcasts to peer accounts.
  private final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  private final AccountManager accountManager;

  // A Map of addresses to all RoutableAccounts that are currently being tracked by this routing service.
  private final Map<InterledgerAddress, RoutableAccount> trackedAccounts;

  // Holds routing information for a given peer, which is a remote ILPv4 node that this connector is peering with.
  //private final Map<InterledgerAddress, Peer> peers;
  private final RoutingTableEntryComparator routingTableEntryComparator;

  // Stores the unique identifier of a Runnable so that if unregister is called, that particular handler
  // can be removed from the callback listeners, resulting in an account no longer being registered/tracked by this
  // service.
  private Map<InterledgerAddress, Runnable> unregisterAccountCallbacks;

  /**
   * Required-args Constructor.
   */
  public DefaultRoutingService(
    final CodecContext codecContext,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager
  ) {
    this.codecContext = Objects.requireNonNull(codecContext);
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);

    this.localRoutingTable = new InMemoryRoutingTable();
    this.incomingRoutingTable = new InMemoryIncomingRouteForwardRoutingTable();
    this.outgoingRoutingTable = new InMemoryRouteUpdateForwardRoutingTable();
    this.accountManager = Objects.requireNonNull(accountManager);

    this.trackedAccounts = Maps.newConcurrentMap();
    this.unregisterAccountCallbacks = Maps.newConcurrentMap();
    this.routingTableEntryComparator = new RoutingTableEntryComparator(accountManager);
    this.routingServiceListenerId = UUID.randomUUID();
  }

  /**
   * Required-args Constructor.
   */
  @VisibleForTesting
  protected DefaultRoutingService(
    final CodecContext codecContext,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final RoutingTable<Route> localRoutingTable,
    final ForwardingRoutingTable<IncomingRoute> incomingRoutingTable,
    final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    final AccountManager accountManager
  ) {
    this.codecContext = Objects.requireNonNull(codecContext);
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.localRoutingTable = Objects.requireNonNull(localRoutingTable);
    this.incomingRoutingTable = Objects.requireNonNull(incomingRoutingTable);
    this.outgoingRoutingTable = Objects.requireNonNull(outgoingRoutingTable);

    this.accountManager = Objects.requireNonNull(accountManager);
    this.unregisterAccountCallbacks = Maps.newConcurrentMap();
    this.trackedAccounts = Maps.newConcurrentMap();
    this.routingTableEntryComparator = new RoutingTableEntryComparator(accountManager);
    this.routingServiceListenerId = UUID.randomUUID();
  }

  public void start() {
    this.reloadLocalRoutes();

    // Add each account to this service so that each can be tracked. For example, if a lpi2 for a given account
    // disconnects, then we want this RoutingService to know about it so that payments aren't routed through a
    // disconencted lpi2.

    this.accountManager.getAllAccountSettings()
      .map(AccountSettings::getInterledgerAddress)
      .forEach(this::registerAccount);
  }

  @PreDestroy
  public void shutdown() {
    this.accountManager.getAllAccountSettings()
      .map(AccountSettings::getInterledgerAddress)
      .forEach(this::unregisterAccount);
  }

  /**
   * Register this service to respond to connect/disconnect events that may be emitted from a {@link Plugin}, and then
   * add the account to this service's internal machinery.
   */
  public void registerAccount(final InterledgerAddress peerAccount) {
    Objects.requireNonNull(peerAccount);

    if (this.unregisterAccountCallbacks.containsKey(peerAccount)) {
      logger.warn("Peer Account `{}` was already tracked!", peerAccount);
      return;
    }

    final Plugin plugin = this.accountManager.getPluginManager().safeGetPlugin(peerAccount);

    // When registering an account, we should remove any callbacks that might already exist.
    this.unregisterAccountCallbacks.put(peerAccount, () -> {
      plugin.removePluginEventListener(routingServiceListenerId);
    });

    // Tracked plugins should not enter service until they connect. Additionally, if a tracked plugin disconnects, it
    // should be removed from operation until it reconnects.
    plugin.addPluginEventListener(routingServiceListenerId, new PluginEventListener() {
      @Override
      public void onConnect(PluginConnectedEvent pluginConnectedEvent) {
        if (!plugin.isConnected()) {
          // some plugins don't set `isConnected() = true` before emitting the
          // connect event, setImmediate has a good chance of working.
          logger
            .error("Plugin emitted connect, but then returned false for isConnected, broken plugin. peerAccount={}",
              pluginConnectedEvent.getPlugin().getPluginSettings().getPeerAccountAddress());
          //setImmediate(() => this.add(accountId))
        } else {
          addTrackedAccount(pluginConnectedEvent.getPlugin().getPluginSettings().getPeerAccountAddress());
        }
      }

      @Override
      public void onDisconnect(PluginDisconnectedEvent pluginDisconnectedEvent) {
        removeAccount(pluginDisconnectedEvent.getPlugin().getPluginSettings().getPeerAccountAddress());
      }

      @Override
      public void onError(PluginErrorEvent pluginErrorEvent) {
        logger.error(pluginErrorEvent.getError().getMessage(), pluginErrorEvent.getError());
      }
    });
  }

  /**
   * Untrack a peer account address from participating in Routing.
   *
   * @param peerAccount The address of a remote peer that can have packets routed to it.
   */
  public void unregisterAccount(final InterledgerAddress peerAccount) {
    this.removeAccount(peerAccount);

    Optional.ofNullable(this.unregisterAccountCallbacks.get(peerAccount))
      .ifPresent(untrackRunnable -> {
        // Run the Runnable that was configured when the plugin was registered (see TrackAccount).
        untrackRunnable.run();
      });
  }

  // This is happening via the IlpNodeEvent system...

  //  /**
  //   * If an AccountManager is present in the system running this routing service, then this service can react to those
  //   * events by updating the routing table.
  //   *
  //   * @param accountAddedEvent
  //   */
  //  @EventListener(condition = "#event.name == T(com.sappenin.interledger.ilpv4.connector.events.AccountAddedEvent).NAME")
  //  public void handleAccountAdded(AccountAddedEvent accountAddedEvent) {
  //
  //
  //  }
  //
  //  /**
  //   * If an AccountManager is present in the system running this routing service, then this service can react to those
  //   * events by updating the routing table.
  //   *
  //   * @param accountRemovedEvent
  //   */
  //  @EventListener(
  //    condition = "#event.name == T(com.sappenin.interledger.ilpv4.connector.events.AccountRemotedEvent).NAME"
  //  )
  //  public void handleAccountRemoved(AccountRemovedEvent accountRemovedEvent) {
  //    System.out.println("Handling generic event (conditional).");
  //  }

  /**
   * Adds a remote peerAccountAddress to this Routing Service. This method is called in response to a tracking call.
   *
   * @param peerAccountAddress
   */
  @VisibleForTesting
  protected void addTrackedAccount(final InterledgerAddress peerAccountAddress) {
    Objects.requireNonNull(peerAccountAddress);

    final AccountSettings peerAccountSettings = this.accountManager.getAccountSettings(peerAccountAddress)
      // TODO: This condition never happen, so RuntimeException is probably sufficient. However, consider an
      // InterledgerException instead?
      .orElseThrow(() -> new RuntimeException("Account not found!"));

    // Check if this account is a parent account. If so, update the routing table appropriately (e.g., if this is a
    // parent account, and its the primary parent account, then add a global route for this account).
    if (
      peerAccountSettings.getRelationship() == AccountSettings.AccountRelationship.PARENT
        && this.accountManager.getPrimaryParentAccountSettings().get().getInterledgerAddress()
        .equals(peerAccountSettings.getInterledgerAddress())
    ) {
      // Add a default global route for this account...
      this.setDefaultRoute(peerAccountSettings.getInterledgerAddress());
    }

    final boolean sendRoutes = this.shouldSendRoutes(peerAccountSettings);
    final boolean receiveRoutes = this.shouldReceiveRoutes(peerAccountSettings);
    if (!sendRoutes && !receiveRoutes) {
      logger.warn("Not sending nor receiving routes for peer. accountId={}", peerAccountAddress);
      return;
    }

    this.getTrackedAccount(peerAccountAddress)
      .map(existingPeer -> {
        // Every time we reconnect, we'll send a new getRoute control message to make sure they are still sending us
        // routes.
        existingPeer.getCcpReceiver().sendRouteControl();
        return existingPeer;
      })
      .orElseGet(() -> {
        // The Account in question did not have an existing peer in this routing service, so create a new Peer and
        // initialize it.
        final Plugin plugin = this.accountManager.getPluginManager().safeGetPlugin(peerAccountAddress);
        logger.debug("Adding peer. accountId={} isSendRoutes={} isReceiveRoutes={}", peerAccountAddress, sendRoutes,
          receiveRoutes);
        final RoutableAccount newPeer = ImmutableRoutableAccount.builder()
          .accountSettings(peerAccountSettings)
          .ccpSender(constructCcpSender(plugin))
          .ccpReceiver(constructCcpReceiver(plugin))
          .build();
        this.setTrackedAccount(newPeer);

        // Always send a new RoutControl request to the remote peer, but only if it's connected.
        newPeer.getCcpReceiver().sendRouteControl();

        // This is somewhat expensive, so only do this if the peer was newly constructed...in other words, we don't
        // want to reload the local routing tables if a plugin merely disconnected and reconnected, but nothing
        // intrinsically changed about the routing tables that wasn't handled by those listeners.
        this.reloadLocalRoutes();

        return newPeer;
      });
  }

  private CcpSender constructCcpSender(final Plugin plugin) {
    Objects.requireNonNull(plugin);
    return new DefaultCcpSender(
      this.connectorSettingsSupplier,
      this.outgoingRoutingTable,
      this.accountManager,
      this.codecContext,
      plugin
    );
  }

  private CcpReceiver constructCcpReceiver(final Plugin plugin) {
    Objects.requireNonNull(plugin);
    return new DefaultCcpReceiver(
      this.connectorSettingsSupplier,
      this.incomingRoutingTable,
      this.codecContext,
      plugin
    );
  }

  /**
   * Determines if the plugin configured for the account in {@code peerAccountSettings} should send routes to the remote
   * peer account.
   *
   * @param peerAccountSettings An instance of {@link AccountSettings} for a remote peer account.
   *
   * @return {@code true} if the plugin is configured to send routes, {@code false} otherwise.
   */
  private boolean shouldSendRoutes(final AccountSettings peerAccountSettings) {
    Objects.requireNonNull(peerAccountSettings);
    if (peerAccountSettings.getRelationship().equals(AccountSettings.AccountRelationship.CHILD)) {
      return SHOULD_NOT_SEND_ROUTES;
    } else {
      return peerAccountSettings.getRouteBroadcastSettings().isSendRoutes();
    }
  }

  /**
   * Determines if the plugin configured for the account in {@code peerAccountSettings} should receive routes from the
   * remote peer account.
   *
   * @param peerAccountSettings An instance of {@link AccountSettings} for a remote peer account.
   *
   * @return {@code true} if the plugin is configured to receive routes, {@code false} otherwise.
   */
  private boolean shouldReceiveRoutes(final AccountSettings peerAccountSettings) {
    Objects.requireNonNull(peerAccountSettings);
    if (peerAccountSettings.getRelationship().equals(AccountSettings.AccountRelationship.CHILD)) {
      return SHOULD_NOT_RECEIVE_ROUTES;
    } else {
      return peerAccountSettings.getRouteBroadcastSettings().isReceiveRoutes();
    }
  }

  @VisibleForTesting
  protected void removeAccount(final InterledgerAddress peerAccountAddress) {
    this.getTrackedAccount(peerAccountAddress)
      .ifPresent(peer -> {
        logger.trace("Remove peer. peerId={}", peerAccountAddress);

        // Stop the CcpSender from broadcasting routes...
        peer.getCcpSender().stopBroadcasting();

        // We have to removeEntry the peer before calling updatePrefix on each of its advertised prefixes in order to
        // find the next best getRoute.
        this.trackedAccounts.remove(peerAccountAddress);

        peer.getCcpReceiver().forEachIncomingRoute((incomingRoute) -> {
          // Update all tables that might contain this prefix. This includes local routes
          updatePrefix(incomingRoute.getRoutePrefix());
        });

        this.accountManager.getAccountSettings(peerAccountAddress)
          .map(AccountSettings::getRelationship)
          .filter(accountRelationship -> accountRelationship.equals(AccountSettings.AccountRelationship.CHILD))
          // Only do this if the relationship is CHILD...
          .ifPresent($ -> {
            // TODO: Revisit this!
            //this.updatePrefix(accountManager.toChildAddress(peerAccountAddress))
          });

      });
  }

  /**
   * Update this prefix in both the local and forwarding routing tables.
   *
   * @param addressPrefix
   */
  @VisibleForTesting
  protected void updatePrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    final Optional<Route> newBestRoute = this.getBestPeerRouteForPrefix(addressPrefix);
    if (this.updateLocalRoute(addressPrefix, newBestRoute)) {
      this.updateForwardingRoute(addressPrefix, newBestRoute);
    }
  }

  private boolean updateLocalRoute(
    final InterledgerAddressPrefix addressPrefix, final Optional<Route> newBestRoute
  ) {
    Objects.requireNonNull(addressPrefix);
    Objects.requireNonNull(newBestRoute);

    final Optional<Route> currentBestRoute = this.localRoutingTable.getRouteByPrefix(addressPrefix);

    final Optional<InterledgerAddress> currentNextHop = currentBestRoute.map(nh -> nh.getNextHopAccount());
    final Optional<InterledgerAddress> newBestHop = newBestRoute.map(Route::getNextHopAccount);

    // If the current and next best hops are different, then update the routing tables...
    if (!currentNextHop.equals(newBestHop)) {
      newBestRoute
        .map(nbr -> {
          logger.debug(
            "New best getRoute for prefix. prefix={} oldBest={} newBest={}",
            addressPrefix, currentNextHop, nbr.getNextHopAccount()
          );
          this.localRoutingTable.addRoute(nbr);
          return ROUTES_HAVE_CHANGED;
        })
        .orElseGet(() -> {
          logger.debug("No more getRoute available for prefix. prefix={}", addressPrefix);
          this.localRoutingTable.removeRoute(addressPrefix);
          return ROUTES_HAVE_NOT_CHANGED;
        });
      return ROUTES_HAVE_CHANGED;
    } else {
      return ROUTES_HAVE_NOT_CHANGED;
    }
  }

  private void updateForwardingRoute(
    final InterledgerAddressPrefix addressPrefix, final Optional<? extends Route> newBestLocalRoute
  ) {
    Objects.requireNonNull(addressPrefix);
    Objects.requireNonNull(newBestLocalRoute);

    // If newBestLocalRoute is defined, then map it to itself or empty, depending on various checks.
    // Only if the newBestLocalRoute is present...

    // Given an optionally-present newBestLocalRoute, compute the best next hop address...
    final Optional<InterledgerAddress> newBestNextHop = newBestLocalRoute
      // Map the Local Route to a
      .map(nblr -> {
        // Inject ourselves into the path that we'll send to remote peers...
        final List<InterledgerAddress> newPath = ImmutableList.<InterledgerAddress>builder()
          .add(connectorSettingsSupplier.get().getIlpAddress())
          .addAll(nblr.getPath()).build();

        return ImmutableRoute.builder()
          .from(nblr)
          .path(newPath)
          .auth(Hashing.sha256().hashBytes(nblr.getAuth()).asBytes())
          .build();
      })
      .map(nblr -> {
        final InterledgerAddressPrefix globalPrefix = connectorSettingsSupplier.get().getGlobalPrefix();
        final boolean hasGlobalPrefix = addressPrefix.getRootPrefix().equals(globalPrefix);
        final boolean isDefaultRoute = addressPrefix.startsWith(globalPrefix);

        // Don't advertise local customer routes that we originated. Packets for these destinations should still
        // reach us because we are advertising our own address as a prefix.
        final boolean isLocalCustomerRoute =
          addressPrefix.getValue().startsWith(this.connectorSettingsSupplier.get().getIlpAddress().getValue()) &&
            nblr.getPath().size() == 1;

        final boolean canDragonFilter = false; // TODO: Dragon!

        // We don't advertise getRoute if any of the following are true:
        // 1. Route doesn't start with the global prefix.
        // 2. The prefix _is_ the global prefix (i.e., the default getRoute).
        // 3. The prefix is for a local-customer getRoute.
        // 4. The prefix can be dragon-filtered.
        if (!hasGlobalPrefix || isDefaultRoute || isLocalCustomerRoute || canDragonFilter) {
          // This will map to Optional.empty above...
          return null;
        } else {
          return nblr;
        }
      })
      .map(Route::getNextHopAccount);

    // Only if there's a newBestNextHop, update the forwarding tables...
    newBestNextHop.ifPresent(nbnh -> {

      final Optional<RouteUpdate> currentBest = this.outgoingRoutingTable.getRouteForPrefix(addressPrefix);
      final Optional<InterledgerAddress> currentBestNextHop = currentBest
        .map(RouteUpdate::getRoute)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Route::getNextHopAccount);

      // There's a new NextBestHop, so update the forwarding table, but only if it's different from the optionally
      // present currentBestNextHop.
      if (!currentBestNextHop.isPresent() || !currentBestNextHop.get().equals(nbnh)) {
        final int newEpoch = this.outgoingRoutingTable.getCurrentEpoch() + 1;

        final RouteUpdate newBestRouteUpdate = ImmutableRouteUpdate.builder()
          .routePrefix(addressPrefix)
          .route(newBestLocalRoute)
          .epoch(newEpoch)
          .build();

        // Add the getRoute update to the outoing routing table so that it will be sent to remote peers on the next CCP
        // send operation.
        this.outgoingRoutingTable.addRoute(addressPrefix, newBestRouteUpdate);
        logger.debug("Logging getRoute update. update={}", newBestRouteUpdate);

        // If there's a current-best, null-out the getEpoch.
        currentBest.ifPresent(ru -> outgoingRoutingTable.resetEpochValue(ru.getEpoch()));

        // Set the new best into the new epoch.
        outgoingRoutingTable.setEpochValue(newEpoch, newBestRouteUpdate);

        // Update the epoch after updating the log so that the Route forwarder doesn't think there's new things to
        // send before they're actually added.
        outgoingRoutingTable.compareAndSetCurrentEpoch(this.outgoingRoutingTable.getCurrentEpoch(), newEpoch);

        // If there's a new best local route, then we need to re-check any prefixes that start with this prefix to see
        // if we can apply DRAGON filtering.
        //
        // Note that we do this check *after* we have added the new getRoute above.
        this.outgoingRoutingTable.getKeysStartingWith(addressPrefix).stream()
          // If the subPrefix equals the addressPrefix, then ignore it.
          .filter(subPrefix -> !subPrefix.equals(addressPrefix))
          // Otherwise, update the outgoing RoutingTable.
          .forEach(subPrefix -> {
            // Only update the forward routing table if there is a route present (we don't want to removeEntry routes when
            // dragon filtering...)
            this.outgoingRoutingTable.getRouteForPrefix(subPrefix)
              .ifPresent(routeUpdate -> this.updateForwardingRoute(subPrefix, routeUpdate.getRoute()));
          });
      }
    });
  }

  /**
   * Reload the local routing table based upon our current peer settings.
   */
  @VisibleForTesting
  protected void reloadLocalRoutes() {
    logger.debug("Entering #reloadLocalRoutes...");

    // Rese the local routing table.
    this.localRoutingTable.reset();

    // Local Accounts?
    //this.accountManager.getAllAccountSettings();


    // Add a route for our own address....
    final InterledgerAddress ourAddress = this.connectorSettingsSupplier.get().getIlpAddress();
    this.localRoutingTable.addRoute(
      //      InterledgerAddressPrefix.from(ourAddress),
      ImmutableRoute.builder()
        .nextHopAccount(ourAddress)
        .routePrefix(InterledgerAddressPrefix.from(ourAddress))
        .auth(hmac(this.connectorSettingsSupplier.get().getDefaultRouteSettings().getRoutingSecret(),
          InterledgerAddressPrefix.from(ourAddress)))
        // empty path.
        // never expires
        .build()
    );

    // Determine the default Route, and add it to the local routing table.
    final InterledgerAddress nextHopForDefaultRoute;
    if (connectorSettingsSupplier.get().getDefaultRouteSettings().useParentForDefaultRoute()) {
      nextHopForDefaultRoute = this.accountManager.getAllAccountSettings()
        .filter(accountSettings -> accountSettings.getRelationship().equals(AccountSettings.AccountRelationship.PARENT))
        .findFirst()
        .map(AccountSettings::getInterledgerAddress)
        .orElseThrow(() -> new RuntimeException("Connector was configured to use the Parent account " +
          "as the nextHop for the default route, but no Parent Account was configured!"));
    } else {
      nextHopForDefaultRoute = connectorSettingsSupplier.get().getDefaultRouteSettings().getDefaultRoute()
        .orElseThrow(() -> new RuntimeException("Connector was configured to use a default address as the nextHop " +
          "for the default route, but no Account was configured for this address!"));
    }
    this.localRoutingTable.addRoute(
      //InterledgerAddressPrefix.GLOBAL,
      ImmutableRoute.builder()
        .routePrefix(InterledgerAddressPrefix.GLOBAL)
        .nextHopAccount(nextHopForDefaultRoute)
        // empty path
        .auth(hmac(this.connectorSettingsSupplier.get().getDefaultRouteSettings().getRoutingSecret(),
          InterledgerAddressPrefix.GLOBAL))
        // empty path.
        // never expires
        .build()
    );

    // For each local account that is a child...
    this.accountManager.getAllAccountSettings()
      .filter(localAccount -> localAccount.getRelationship().equals(AccountSettings.AccountRelationship.CHILD))
      .map(AccountSettings::getInterledgerAddress)
      .forEach(localAccountAddress -> {
        final InterledgerAddressPrefix childAddressAsPrefix =
          InterledgerAddressPrefix.from(this.accountManager.toChildAddress(localAccountAddress));
        localRoutingTable.addRoute(
          //childAddressAsPrefix,
          ImmutableRoute.builder()
            .routePrefix(childAddressAsPrefix)
            .nextHopAccount(localAccountAddress)
            // No Path
            .auth(hmac(
              this.connectorSettingsSupplier.get().getDefaultRouteSettings().getRoutingSecret(), childAddressAsPrefix)
            )
            .build()
        );
      });

    // Local prefixes Stream
    final Stream<InterledgerAddressPrefix> localPrfixesStream =
      StreamSupport.stream(this.localRoutingTable.getAllPrefixes().spliterator(), false);

    // Configured prefixes Stream (from Static routes)
    final Stream<InterledgerAddressPrefix> configuredPrefixesStream =
      this.connectorSettingsSupplier.get().getDefaultRouteSettings().getStaticRoutes().stream()
        .map(StaticRoute::getTargetPrefix);
    // Update all prefixes...
    Stream.concat(localPrfixesStream, configuredPrefixesStream).forEach(this::updatePrefix);
  }

  @VisibleForTesting
  protected Optional<Route> getBestPeerRouteForPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);

    // configured routes have highest priority

    // then find the configuredNextBestPeerRoute
    //final Optional<Route> configuredNextBestPeerRoute =
    return Optional.ofNullable(
      connectorSettingsSupplier.get().getDefaultRouteSettings().getStaticRoutes().stream()
        .filter(staticRoute -> staticRoute.getTargetPrefix().equals(addressPrefix))
        .findFirst()
        // If there's a static getRoute, then try to find the account that exists for that getRoute...
        .map(staticRoute -> accountManager.getAccountSettings(staticRoute.getPeerAddress()).orElseGet(() -> {
          logger.warn("Ignoring configured getRoute, account does not exist. prefix={} peerAccountId={}",
            staticRoute.getTargetPrefix(), staticRoute.getPeerAddress());
          return null;
        }))
        // If there's a static getRoute, and the account exists...
        .map(accountSettings -> {
          // Otherwise, if the account exists, then we should return a new getRoute to the peer.
          final Route route = ImmutableRoute.builder()
            .path(Collections.emptyList())
            .nextHopAccount(accountSettings.getInterledgerAddress())
            .auth(hmac(connectorSettingsSupplier.get().getDefaultRouteSettings().getRoutingSecret(), addressPrefix))
            .build();
          return route;
        })
        .orElseGet(() -> {
            //...then look in the receiver.
            // If we getEntry here, there was no statically-configured getRoute, _or_, there was a statically configured getRoute
            // but no account existed. Either way, look for a local getRoute.
            return localRoutingTable.getRouteByPrefix(addressPrefix)
              .orElseGet(() -> {
                // If we getEntry here, there was no local getRoute, so search through all tracked accounts and sort all
                // of the routes to find the shortest-path (i.e., lowest weight) route that will work for `addressPrefix`.
                return trackedAccounts.values().stream()
                  .map(RoutableAccount::getCcpReceiver)
                  .map(ccpReceiver -> ccpReceiver.getRouteForPrefix(addressPrefix).orElse(null))
                  .sorted(routingTableEntryComparator)
                  .collect(Collectors.toList()).stream()
                  .findFirst()
                  .map(bestRoute -> ImmutableRoute.builder()
                    .routePrefix(bestRoute.getRoutePrefix())
                    .nextHopAccount(bestRoute.getPeerAddress())
                    .path(bestRoute.getPath())
                    .auth(bestRoute.getAuth())
                    .build()
                  )
                  .orElse(null);
              });
          }
        )
    );
  }

  /**
   * Create an HMAC of the routing secret and address prefix using Hmac SHA256.
   */
  private byte[] hmac(String routingSecret, InterledgerAddressPrefix addressPrefix) {
    try {
      return Hashing
        .hmacSha256(routingSecret.getBytes(UTF_8))
        .hashBytes(addressPrefix.getValue().getBytes(UTF_8)).asBytes();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setTrackedAccount(final RoutableAccount routableAccount) {
    Objects.requireNonNull(routableAccount);

    if (
      trackedAccounts.putIfAbsent(routableAccount.getAccountSettings().getInterledgerAddress(), routableAccount) != null
    ) {
      throw new RuntimeException(String.format(
        "Account with InterledgerAddress `%s` is already being tracked for routing purposes",
        routableAccount.getAccountSettings().getInterledgerAddress()
      ));
    }
  }

  @Override
  public Optional<RoutableAccount> getTrackedAccount(final InterledgerAddress peerAccountAddress) {
    Objects.requireNonNull(peerAccountAddress);
    return Optional.ofNullable(this.trackedAccounts.get(peerAccountAddress));
  }

  ////////////////////////
  // PaymentRouter Methods
  ////////////////////////

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);
    return this.localRoutingTable.findNextHopRoute(finalDestinationAddress);
  }

  @Override
  public RoutingTable<Route> getRoutingTable() {
    return this.localRoutingTable;
  }

  @Override
  public void setDefaultRoute(final InterledgerAddress defaultDestinationAddress) {
    Objects.requireNonNull(defaultDestinationAddress);

    // Add a default global route for this account...
    final Route defaultGlobalRoute = ImmutableRoute.builder()
      .routePrefix(InterledgerAddressPrefix.GLOBAL)
      .expiresAt(Instant.MAX)
      .nextHopAccount(defaultDestinationAddress)
      .build();
    this.localRoutingTable.addRoute(defaultGlobalRoute);

    final Route defaultTestRoute =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST).build();
    this.localRoutingTable.addRoute(defaultTestRoute);

    final Route defaultTest1Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST1).build();
    this.localRoutingTable.addRoute(defaultTest1Route);

    final Route defaultTest2Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST2).build();
    this.localRoutingTable.addRoute(defaultTest2Route);

    final Route defaultTest3Route =
      ImmutableRoute.builder().from(defaultGlobalRoute).routePrefix(InterledgerAddressPrefix.TEST3).build();
    this.localRoutingTable.addRoute(defaultTest3Route);
  }

  protected class RoutingTableEntryComparator implements Comparator<IncomingRoute> {
    private final AccountManager accountManager;

    public RoutingTableEntryComparator(final AccountManager accountManager) {
      this.accountManager = Objects.requireNonNull(accountManager);
    }

    @Override
    public int compare(IncomingRoute entryA, IncomingRoute entryB) {
      // Null checks...
      if (entryA == null && entryB == null) {
        return 0;
      } else if (entryA == null) {
        return 1;
      } else if (entryB == null) {
        return -1;
      }

      // First sort by peer weight
      int weight1 = getWeight(entryA);
      int weight2 = getWeight(entryB);

      if (weight1 != weight2) {
        return weight2 - weight1;
      }

      // Then sort by path length
      int sizePathA = entryA.getPath().size();
      int sizePathB = entryB.getPath().size();

      if (sizePathA != sizePathB) {
        return sizePathA - sizePathB;
      }

      // Finally, tie-break by accountId
      return entryA.getPeerAddress().getValue().compareTo(entryB.getPeerAddress().getValue());
    }

    @VisibleForTesting
    protected int getWeight(final IncomingRoute route) {
      return this.accountManager.getAccountSettings(route.getPeerAddress())
        .orElseThrow(() -> new RuntimeException(
          String.format("Account should have existed: %s", route.getPeerAddress())
        ))
        .getRelationship()
        .getWeight();
    }
  }
}
