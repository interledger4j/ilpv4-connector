package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import com.sappenin.interledger.ilpv4.connector.StaticRoute;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.connector.link.events.LinkDisconnectedEvent;
import org.interledger.connector.link.events.LinkErrorEvent;
import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.encoding.asn.framework.CodecContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.sappenin.interledger.ilpv4.connector.routing.Route.HMAC;

/**
 * An implementation of {@link ExternalRoutingService} that manages an in-memory routing table used to route packets
 * between publicly accessible peers (i.e., other ILP nodes that we might want to forward public ILP network routing
 * information to).
 */
public class InMemoryExternalRoutingService implements ExternalRoutingService, LinkEventListener {

  private static final boolean SHOULD_SEND_ROUTES = true;
  private static final boolean SHOULD_NOT_SEND_ROUTES = false;
  private static final boolean SHOULD_RECEIVE_ROUTES = true;
  private static final boolean SHOULD_NOT_RECEIVE_ROUTES = false;
  private static final boolean ROUTES_HAVE_CHANGED = true;
  private static final boolean ROUTES_HAVE_NOT_CHANGED = false;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final EventBus eventBus;

  private final CodecContext ccpCodecContext;

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  // Local routing table, used for actually routing packets.
  private final RoutingTable<Route> localRoutingTable;

  // Used for routes that this connector has received from peer accounts.
  private final ForwardingRoutingTable<IncomingRoute> incomingRoutingTable;

  // Master outgoing routing table, used for routes that this connector broadcasts to peer accounts.
  private final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  private final AccountIdResolver accountIdResolver;
  private final AccountManager accountManager;

  // A Map of addresses to all RoutableAccounts that are currently being tracked by this routing service.
  private final Map<AccountId, RoutableAccount> trackedAccounts;

  // Holds routing information for a given peer, which is a remote ILPv4 node that this connector is peering with.
  //private final Map<InterledgerAddress, Peer> peers;
  private final RoutingTableEntryComparator routingTableEntryComparator;

  // Stores the unique identifier of a Runnable so that if unregister is called, that particular handler
  // can be removed from the callback listeners, resulting in an account no longer being registered/tracked by this
  // service.
  private Map<AccountId, Runnable> unregisterAccountCallbacks;

  /**
   * Required-args Constructor.
   */
  public InMemoryExternalRoutingService(
    final EventBus eventBus,
    final CodecContext ccpCodecContext,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountManager accountManager,
    final AccountIdResolver accountIdResolver
  ) {
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this); // Required in order to add/remove routes on Link Connect/Disconnects

    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);

    this.localRoutingTable = new InMemoryRoutingTable();
    this.incomingRoutingTable = new InMemoryIncomingRouteForwardRoutingTable();
    this.outgoingRoutingTable = new InMemoryRouteUpdateForwardRoutingTable();
    this.accountManager = Objects.requireNonNull(accountManager);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);

    this.trackedAccounts = Maps.newConcurrentMap();
    this.unregisterAccountCallbacks = Maps.newConcurrentMap();
    this.routingTableEntryComparator = new RoutingTableEntryComparator(accountManager);
  }

  /**
   * Required-args Constructor.
   */
  @VisibleForTesting
  protected InMemoryExternalRoutingService(
    final EventBus eventBus,
    final CodecContext ccpCodecContext,
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final RoutingTable<Route> localRoutingTable,
    final ForwardingRoutingTable<IncomingRoute> incomingRoutingTable,
    final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    final AccountManager accountManager,
    final AccountIdResolver accountIdResolver
  ) {
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this);

    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.localRoutingTable = Objects.requireNonNull(localRoutingTable);
    this.incomingRoutingTable = Objects.requireNonNull(incomingRoutingTable);
    this.outgoingRoutingTable = Objects.requireNonNull(outgoingRoutingTable);

    this.accountManager = Objects.requireNonNull(accountManager);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.unregisterAccountCallbacks = Maps.newConcurrentMap();
    this.trackedAccounts = Maps.newConcurrentMap();
    this.routingTableEntryComparator = new RoutingTableEntryComparator(accountManager);
  }

  @Override
  public void start() {
    this.reloadLocalRoutes();

    // Add each non-internal account to this service so that each can be tracked. For example, if a lpi2 for a given
    // account disconnects, then we want this ExternalRoutingService to know about it so that payments aren't routed through a
    // disconencted lpi2.

    this.accountManager.getAllAccounts()
      .map(Account::getAccountSettings)
      .filter(accountSettings -> accountSettings.isInternal() == false)
      .map(AccountSettings::getAccountId)
      .forEach(this::registerAccount);
  }

  @Override
  @PreDestroy
  public void shutdown() {
    this.accountManager.getAllAccounts()
      .map(Account::getAccountSettings)
      .map(AccountSettings::getAccountId)
      .forEach(this::unregisterAccount);
  }

  /**
   * Register this service to respond to connect/disconnect events that may be emitted from a {@link Link}, and then add
   * the accountId to this service's internal machinery.
   */
  public void registerAccount(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    if (this.unregisterAccountCallbacks.containsKey(accountId)) {
      logger.warn("Peer Account `{}` was already tracked!", accountId);
      return;
    }

    final Link<?> dataLink = this.accountManager.safeGetAccount(accountId).getLink();

    final LinkEventListener dataLinkEventListener = new LinkEventListener() {
      @Override
      public void onConnect(LinkConnectedEvent linkConnectedEvent) {
        final AccountId accountId = AccountId.of(dataLink.getLinkId().get().value());
        if (!dataLink.isConnected()) {
          // some links don't set `isConnected() = true` before emitting the
          // connect event, setImmediate has a good chance of working.
          logger.error(
            "Link emitted connect, but then returned false for isConnected, broken dataLink. accountId={}",
            accountId
          );
        } else {
          addTrackedAccount(accountId);
        }
      }

      @Override
      public void onDisconnect(LinkDisconnectedEvent linkDisconnectedEvent) {
        final AccountId accountId = accountIdResolver.resolveAccountId(dataLink);
        removeAccount(accountId);
      }

      @Override
      public void onError(LinkErrorEvent linkErrorEvent) {
        logger.error(linkErrorEvent.getError().getMessage(), linkErrorEvent.getError());
      }
    };

    // When registering an accountId, we should remove any callbacks that might already exist.
    this.unregisterAccountCallbacks.put(accountId, () -> dataLink.removeLinkEventListener(dataLinkEventListener));

    // Tracked accounts should not enter service until they connect. Additionally, if a tracked dataLink disconnects, it
    // should be removed from operation until it reconnects.
    dataLink.addLinkEventListener(dataLinkEventListener);

    // If the dataLink for the account above is already connected, then the account/dataLink won't become eligible for
    // routing without this extra check.
    if (dataLink.isConnected()) {
      this.addTrackedAccount(accountId);
    }
  }

  /**
   * Untrack a peer account address from participating in Routing.
   *
   * @param account The address of a remote peer that can have packets routed to it.
   */
  public void unregisterAccount(final AccountId account) {
    this.removeAccount(account);

    Optional.ofNullable(this.unregisterAccountCallbacks.get(account))
      .ifPresent(untrackRunnable -> {
        // Run the Runnable that was configured when the link was registered (see TrackAccount).
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
   * Adds a remote accountId to this Routing Service. This method is called in response to a tracking call.
   *
   * @param accountId
   */
  @VisibleForTesting
  protected void addTrackedAccount(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    final Account account = this.accountManager.getAccount(accountId)
      // TODO: This condition never happens, so RuntimeException is probably sufficient. However, consider an
      // InterledgerException instead?
      .orElseThrow(() -> new RuntimeException("Account not found!"));

    // Check if this account is a parent account. If so, update the routing table appropriately (e.g., if this is a
    // parent account, and its the primary parent account, then add a global route for this account).
    if (
      account.isParentAccount() &&
        this.accountManager.getPrimaryParentAccount().get().getAccountSettings().getAccountId()
          .equals(account.getAccountSettings().getAccountId())
    ) {
      // Add a default global route for this account...
      this.setDefaultRoute(account.getAccountSettings().getAccountId());
    }

    final boolean sendRoutes = this.shouldSendRoutes(account);
    final boolean receiveRoutes = this.shouldReceiveRoutes(account);
    if (!sendRoutes && !receiveRoutes) {
      logger.warn("Not sending nor receiving routes for peer. accountId={}", accountId);
      return;
    }

    this.getTrackedAccount(accountId)
      .map(existingPeer -> {
        // Every time we reconnect, we'll send a new route control message to make sure they are still sending us
        // routes.
        existingPeer.getCcpReceiver().sendRouteControl();
        return existingPeer;
      })
      .orElseGet(() -> {
        // The Account in question did not have an existing peer in this routing service, so create a new Peer and
        // initialize it.
        final Link<?> link = this.accountManager.safeGetAccount(accountId).getLink();
        logger.debug("Adding peer. accountId={} sendRoutes={} isReceiveRoutes={}", accountId, sendRoutes,
          receiveRoutes);
        final RoutableAccount newPeer = ImmutableRoutableAccount.builder()
          .account(account)
          .ccpSender(constructCcpSender(account.getAccountSettings().getAccountId(), link))
          .ccpReceiver(constructCcpReceiver(account.getAccountSettings().getAccountId(), link))
          .build();
        this.setTrackedAccount(newPeer);

        // Always send a new RoutControl request to the remote peer, but only if it's connected.
        newPeer.getCcpReceiver().sendRouteControl();

        // This is somewhat expensive, so only do this if the peer was newly constructed...in other words, we don't
        // want to reload the local routing tables if a link merely disconnected and reconnected, but nothing
        // intrinsically changed about the routing tables that wasn't handled by those listeners.
        this.reloadLocalRoutes();

        return newPeer;
      });
  }

  private CcpSender constructCcpSender(final AccountId peerAccountId, final Link link) {
    Objects.requireNonNull(peerAccountId);
    Objects.requireNonNull(link);
    return new DefaultCcpSender(
      this.connectorSettingsSupplier, peerAccountId, link, this.outgoingRoutingTable, this.accountManager,
      this.ccpCodecContext
    );
  }

  private CcpReceiver constructCcpReceiver(final AccountId peerAccountId, final Link link) {
    Objects.requireNonNull(peerAccountId);
    Objects.requireNonNull(link);
    return new DefaultCcpReceiver(
      this.connectorSettingsSupplier, peerAccountId, link, this.incomingRoutingTable, this.ccpCodecContext
    );
  }

  /**
   * Determines if the link configured for the account in {@code account} should send routes to the remote peer
   * account.
   *
   * @param account An instance of {@link AccountSettings} for a remote peer account.
   *
   * @return {@code true} if the link is configured to send routes, {@code false} otherwise.
   */
  private boolean shouldSendRoutes(final Account account) {
    Objects.requireNonNull(account);
    if (account.isChildAccount()) {
      return SHOULD_NOT_SEND_ROUTES;
    } else {
      return account.getAccountSettings().isSendRoutes();
    }
  }

  /**
   * Determines if the link configured for the account in {@code account} should receive routes from the remote peer
   * account.
   *
   * @param account An instance of {@link AccountSettings} for a remote peer account.
   *
   * @return {@code true} if the link is configured to receive routes, {@code false} otherwise.
   */
  private boolean shouldReceiveRoutes(final Account account) {
    Objects.requireNonNull(account);
    if (account.isChildAccount()) {
      return SHOULD_NOT_RECEIVE_ROUTES;
    } else {
      return account.getAccountSettings().isReceiveRoutes();
    }
  }

  @VisibleForTesting
  protected void removeAccount(final AccountId accountId) {
    this.getTrackedAccount(accountId)
      .ifPresent(peer -> {
        logger.trace("Remove peer. peerId={}", accountId);

        // Stop the CcpSender from broadcasting routes...
        peer.getCcpSender().stopBroadcasting();

        // We have to removeEntry the peer before calling updatePrefix on each of its advertised prefixes in order to
        // find the next best route.
        this.trackedAccounts.remove(accountId);

        peer.getCcpReceiver().forEachIncomingRoute((incomingRoute) -> {
          // Update all tables that might contain this prefix. This includes local routes
          updatePrefix(incomingRoute.getRoutePrefix());
        });

        this.accountManager.getAccount(accountId)
          .map(Account::getAccountSettings)
          .map(AccountSettings::getRelationship)
          .filter(accountRelationship -> accountRelationship.equals(AccountRelationship.CHILD))
          // Only do this if the relationship is CHILD...
          .ifPresent($ -> {
            // TODO: Revisit this!
            //this.updatePrefix(accountManager.toChildAddress(accountId))
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

    final Optional<AccountId> currentNextHop = currentBestRoute.map(nh -> nh.getNextHopAccountId());
    final Optional<AccountId> newBestHop = newBestRoute.map(Route::getNextHopAccountId);

    // If the current and next best hops are different, then update the routing tables...
    if (!currentNextHop.equals(newBestHop)) {
      newBestRoute
        .map(nbr -> {
          logger.debug(
            "New best route for prefix. prefix={} oldBest={} newBest={}",
            addressPrefix, currentNextHop, nbr.getNextHopAccountId()
          );
          this.localRoutingTable.addRoute(nbr);
          return ROUTES_HAVE_CHANGED;
        })
        .orElseGet(() -> {
          logger.debug("No more route available for prefix. prefix={}", addressPrefix);
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
    final Optional<AccountId> newBestNextHop = newBestLocalRoute
      // Map the Local Route to a
      .map(nblr -> {
        // Inject ourselves into the path that we'll send to remote peers...
        final List<InterledgerAddress> newPath = ImmutableList.<InterledgerAddress>builder()
          .add(connectorSettingsSupplier.get().getOperatorAddressSafe())
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
        final boolean isLocalCustomerRoute = addressPrefix.getValue()
          .startsWith(this.connectorSettingsSupplier.get().getOperatorAddressSafe().getValue()) &&
          nblr.getPath().size() == 1;

        final boolean canDragonFilter = false; // TODO: Dragon!

        // We don't advertise route if any of the following are true:
        // 1. Route doesn't start with the global prefix.
        // 2. The prefix _is_ the global prefix (i.e., the default route).
        // 3. The prefix is for a local-customer route.
        // 4. The prefix can be dragon-filtered.
        if (!hasGlobalPrefix || isDefaultRoute || isLocalCustomerRoute || canDragonFilter) {
          // This will map to Optional.empty above...
          return null;
        } else {
          return nblr;
        }
      })
      .map(Route::getNextHopAccountId);

    // Only if there's a newBestNextHop, update the forwarding tables...
    newBestNextHop.ifPresent(nbnh -> {

      final Optional<RouteUpdate> currentBest = this.outgoingRoutingTable.getRouteForPrefix(addressPrefix);
      final Optional<AccountId> currentBestNextHop = currentBest
        .map(RouteUpdate::getRoute)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Route::getNextHopAccountId);

      // There's a new NextBestHop, so update the forwarding table, but only if it's different from the optionally
      // present currentBestNextHop.
      if (!currentBestNextHop.isPresent() || !currentBestNextHop.get().equals(nbnh)) {
        final int newEpoch = this.outgoingRoutingTable.getCurrentEpoch() + 1;

        final RouteUpdate newBestRouteUpdate = ImmutableRouteUpdate.builder()
          .routePrefix(addressPrefix)
          .route(newBestLocalRoute)
          .epoch(newEpoch)
          .build();

        // Add the route update to the outoing routing table so that it will be sent to remote peers on the next CCP
        // send operation.
        this.outgoingRoutingTable.addRoute(addressPrefix, newBestRouteUpdate);
        logger.debug("Logging route update. update={}", newBestRouteUpdate);

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
        // Note that we do this check *after* we have added the new route above.
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

    // Reset the local routing table.
    this.localRoutingTable.reset();

    // Local Accounts?
    //this.accountManager.getAllAccounts();

    // Add a route for our own address....
    //final InterledgerAddress ourAddress = this.connectorSettingsSupplier.get().getOperatorAddress();

    //    this.localRoutingTable.addRoute(
    //      //      InterledgerAddressPrefix.from(ourAddress),
    //      ImmutableRoute.builder()
    //        .nextHopAccountId(AccountId.of("self-ping"))
    //        .routePrefix(InterledgerAddressPrefix.from(ourAddress))
    //        .auth(HMAC(this.connectorSettingsSupplier.get().getGlobalRoutingSettings().getRoutingSecret(),
    //          InterledgerAddressPrefix.from(ourAddress)))
    //        // empty path.
    //        // never expires
    //        .build()
    //    );

    // Determine the default Route, and add it to the local routing table.
    final Optional<AccountId> nextHopForDefaultRoute;
    if (connectorSettingsSupplier.get().getGlobalRoutingSettings().isUseParentForDefaultRoute()) {
      nextHopForDefaultRoute = this.accountManager.getAllAccounts()
        .filter(account -> account.isParentAccount())
        .findFirst()
        .map(Account::getAccountSettings)
        .map(AccountSettings::getAccountId)
        .map(Optional::of)
        .orElseThrow(() -> new RuntimeException("Connector was configured to use the Parent account " +
          "as the nextHop for the default route, but no Parent Account was configured!"));
    } else if (connectorSettingsSupplier.get().getGlobalRoutingSettings().getDefaultRoute().isPresent()) {
      nextHopForDefaultRoute = connectorSettingsSupplier.get().getGlobalRoutingSettings().getDefaultRoute()
        .map(Optional::of)
        .orElseThrow(() -> new RuntimeException("Connector was configured to use a default address as the nextHop " +
          "for the default route, but no Account was configured for this address!"));
    } else {
      logger.warn("No Default Route configured.");
      nextHopForDefaultRoute = Optional.empty();
    }

    nextHopForDefaultRoute.ifPresent(nextHopAccountId ->
      this.localRoutingTable.addRoute(ImmutableRoute.builder()
        .routePrefix(InterledgerAddressPrefix.GLOBAL)
        .nextHopAccountId(nextHopAccountId)
        // empty path
        .auth(HMAC(this.connectorSettingsSupplier.get().getGlobalRoutingSettings().getRoutingSecret(),
          InterledgerAddressPrefix.GLOBAL))
        // empty path.
        // never expires
        .build()
      ));

    // TODO: FIX THIS block per the JS impl...
    // For each local account that is a child...
    this.accountManager.getAllAccounts()
      .filter(Account::isChildAccount)
      .forEach(account -> {
        final InterledgerAddressPrefix childAddressAsPrefix =
          InterledgerAddressPrefix.from(accountManager.toChildAddress(account.getAccountSettings().getAccountId()));
        localRoutingTable.addRoute(
          ImmutableRoute.builder()
            .routePrefix(childAddressAsPrefix)
            .nextHopAccountId(account.getAccountSettings().getAccountId())
            // No Path
            .auth(HMAC(
              this.connectorSettingsSupplier.get().getGlobalRoutingSettings().getRoutingSecret(), childAddressAsPrefix)
            )
            .build()
        );
      });

    // Local prefixes Stream
    final Stream<InterledgerAddressPrefix> localPrefixesStream =
      StreamSupport.stream(this.localRoutingTable.getAllPrefixes().spliterator(), false);

    // Configured prefixes Stream (from Static routes)
    final Stream<InterledgerAddressPrefix> configuredPrefixesStream =
      this.connectorSettingsSupplier.get().getGlobalRoutingSettings().getStaticRoutes().stream()
        .map(StaticRoute::getTargetPrefix);

    // Update all prefixes...
    Stream.concat(localPrefixesStream, configuredPrefixesStream).forEach(this::updatePrefix);
  }

  @VisibleForTesting
  protected Optional<Route> getBestPeerRouteForPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);

    // configured routes have highest priority
    return Optional.ofNullable(
      connectorSettingsSupplier.get().getGlobalRoutingSettings().getStaticRoutes().stream()
        .filter(staticRoute -> staticRoute.getTargetPrefix().equals(addressPrefix))
        .findFirst()
        // If there's a static route, then try to find the account that exists for that route...
        .map(staticRoute -> accountManager.getAccount(staticRoute.getPeerAccountId()).orElseGet(() -> {
          logger.warn("Ignoring configured route, account does not exist. prefix={} accountId={}",
            staticRoute.getTargetPrefix(), staticRoute.getPeerAccountId());
          return null;
        }))
        // If there's a static route, and the account exists...
        .map(accountSettings -> {
          // Otherwise, if the account exists, then we should return a new route to the peer.
          final Route route = ImmutableRoute.builder()
            .path(Collections.emptyList())
            .routePrefix(addressPrefix)
            .nextHopAccountId(accountSettings.getAccountSettings().getAccountId())
            .auth(HMAC(connectorSettingsSupplier.get().getGlobalRoutingSettings().getRoutingSecret(), addressPrefix))
            .build();
          return route;
        })
        .orElseGet(() -> {
            //...then look in the receiver.
            // If we getEntry here, there was no statically-configured route, _or_, there was a statically configured route
            // but no account existed. Either way, look for a local route.
            return localRoutingTable.getRouteByPrefix(addressPrefix)
              .orElseGet(() -> {
                // If we getEntry here, there was no local route, so search through all tracked accounts and sort all
                // of the routes to find the shortest-path (i.e., lowest weight) route that will work for `addressPrefix`.
                return trackedAccounts.values().stream()
                  .map(RoutableAccount::getCcpReceiver)
                  .map(ccpReceiver -> ccpReceiver.getRouteForPrefix(addressPrefix).orElse(null))
                  .filter(route -> route != null)
                  .sorted(routingTableEntryComparator)
                  .collect(Collectors.toList()).stream()
                  .findFirst()
                  .map(bestRoute -> ImmutableRoute.builder()
                    .routePrefix(bestRoute.getRoutePrefix())
                    .nextHopAccountId(bestRoute.getPeerAccountId())
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

  @Override
  public void setTrackedAccount(final RoutableAccount routableAccount) {
    Objects.requireNonNull(routableAccount);

    if (
      trackedAccounts.putIfAbsent(routableAccount.getAccount().getAccountSettings().getAccountId(), routableAccount) != null
    ) {
      throw new RuntimeException(String.format(
        "AccountId `%s` is already being tracked for routing purposes",
        routableAccount.getAccount().getAccountSettings().getAccountId()
      ));
    }
  }

  @Override
  public Optional<RoutableAccount> getTrackedAccount(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return Optional.ofNullable(this.trackedAccounts.get(accountId));
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

  ////////////////////////
  // Link Event Listener
  ////////////////////////

  /**
   * Called to handle an {@link LinkConnectedEvent}.
   *
   * @param event A {@link LinkConnectedEvent}.
   */
  @Override
  @Subscribe
  public void onConnect(LinkConnectedEvent event) {
    // Unregister the account (associated to the link that connected) with the routing service.
    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getLink());
    this.registerAccount(accountId);
  }

  /**
   * Called to handle an {@link LinkDisconnectedEvent}.
   *
   * @param event A {@link LinkDisconnectedEvent}.
   */
  @Override
  @Subscribe
  public void onDisconnect(LinkDisconnectedEvent event) {
    // Unregister the account (associated to the link that disconnected) from the routing service.
    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getLink());
    this.unregisterAccount(accountId);
  }

}
