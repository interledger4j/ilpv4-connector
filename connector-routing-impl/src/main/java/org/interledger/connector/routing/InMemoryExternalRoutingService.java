package org.interledger.connector.routing;

import static org.interledger.connector.routing.Route.HMAC;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.LinkType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>An implementation of {@link ExternalRoutingService} that manages an in-memory routing table used to route
 * packets between publicly accessible peers (i.e., other ILP nodes that we might want to forward public ILP network
 * routing information to).</p>
 *
 * <p>This implementation ultimately populates two routing tables, one for public route-exchange, and another for
 * actual packet routing decisions (i.e., the local routing table).</p>
 *
 * <p>This service has three basic sources of routing data: CCP route updates, statically configured routes, and
 * account-related information used to route packets to child accounts.</p>
 *
 * <p>The implementations organizes these sources of data around the {@link LinkType} of each source using the
 * following rules-set:</p>
 *
 * <ol>
 * <li>Static Routes: When the Connector starts-up, it loads all statically-configured routes and adds
 * them to this routing service.</li>
 * <li>Parent Accounts: Parent accounts connected via IL-DCP will have a routing-table entry added. Other parent
 * accounts that do not use IL-DCP must have a static-route configured in order to be routable.
 * </li>
 * <li>Peer Accounts: All accounts of type {@link AccountRelationship#PEER} may have a static route configured, but
 * this is not automatic. The account may also participate in CCP depending upon account settings in which case
 * additional routes may become available based upon network conditions.</li>
 * <li>Child Accounts: If a particular packet has a destination address that starts-with the address of
 * the Connector's operational address, then the packet is routed using an instance of {@link
 * ChildAccountPaymentRouter}.</li>
 * </ol>
 * </p>
 */
public class InMemoryExternalRoutingService implements ExternalRoutingService {

  private static final boolean ROUTES_HAVE_CHANGED = true;
  private static final boolean ROUTES_HAVE_NOT_CHANGED = false;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final EventBus eventBus;
  private final AccountSettingsRepository accountSettingsRepository;
  private final StaticRoutesRepository staticRoutesRepository;

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final Decryptor decryptor;

  // Local routing table, used for actually routing packets.
  private final RoutingTable<Route> localRoutingTable;

  // This is the master outgoing routing table, used for routes that this connector broadcasts to peer accounts.
  // Note that each CcpReceiver has its own incoming table, but there is only one outgoing table for the entire
  // Connector.
  private final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  // Responsible for managing all outgoing CCP messages...
  private final RouteBroadcaster routeBroadcaster;

  private final RoutingTableEntryComparator routingTableEntryComparator;

  private final ChildAccountPaymentRouter childAccountPaymentRouter;

  // Used to limit the number of warnings emitted for a missing default route.
  private int numDefaultRouteWarnings = 0;

  /**
   * Required-args Constructor.
   */
  public InMemoryExternalRoutingService(
      final EventBus eventBus,
      final Supplier<ConnectorSettings> connectorSettingsSupplier,
      final Decryptor decryptor,
      final AccountSettingsRepository accountSettingsRepository,
      final StaticRoutesRepository staticRoutesRepository,
      final ChildAccountPaymentRouter childAccountPaymentRouter,
      final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
      final RouteBroadcaster routeBroadcaster
  ) {
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this);

    this.routingTableEntryComparator = new RoutingTableEntryComparator(accountSettingsRepository);

    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.decryptor = decryptor;
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.staticRoutesRepository = Objects.requireNonNull(staticRoutesRepository);
    this.childAccountPaymentRouter = Objects.requireNonNull(childAccountPaymentRouter);
    this.localRoutingTable = new InMemoryRoutingTable();

    this.outgoingRoutingTable = Objects.requireNonNull(outgoingRoutingTable);
    this.routeBroadcaster = routeBroadcaster;
  }

  @Override
  public void start() {
    this.initRoutingTables();
  }

  @Override
  public Collection<Route> getAllRoutes() {
    Set<Route> allRoutes = new HashSet<>();
    localRoutingTable.forEach((prefix, route) -> allRoutes.add(route));
    return allRoutes;
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);

    // TODO: An alternative to this setup is to create an Account with an address of `self.childaccounts` and route
    // this packet to a Loopback-like plugin that forwards a packet to the correct account.

    if (this.childAccountPaymentRouter.isChildAccount(finalDestinationAddress)) {
      return childAccountPaymentRouter.findBestNexHop(finalDestinationAddress);
    } else {
      // Child-accounts never make their way into this table. Because of this check, even if a remote node were to
      // get a child-address into this table, it would never be honored.
      return this.localRoutingTable.findNextHopRoute(finalDestinationAddress);
    }
  }

  @Override
  public Set<StaticRoute> getAllStaticRoutes() {
    return staticRoutesRepository.getAllStaticRoutes();
  }


  @Override
  public void deleteStaticRouteByPrefix(InterledgerAddressPrefix prefix) {
    Objects.requireNonNull(prefix);
    if (!staticRoutesRepository.deleteStaticRoute(prefix)) {
      throw new StaticRouteNotFoundProblem(prefix);
    } else {
      localRoutingTable.removeRoute(prefix);
    }
  }

  @Override
  public StaticRoute createStaticRoute(StaticRoute route) {
    Objects.requireNonNull(route);
    try {
      StaticRoute saved = staticRoutesRepository.saveStaticRoute(route);
      addStaticRoute(saved);
      return saved;
    }
    catch(Exception e) {
      if (e.getCause() instanceof ConstraintViolationException) {
        throw new StaticRouteAlreadyExistsProblem(route.addressPrefix());
      }
      throw e;
    }

  }

  /**
   * <p>Initializes (or re-initializes) the routing subsystems used by the Connector to make routing decisions.</p>
   *
   * <p>This method does the following:
   *
   * <ol>
   * <li>Parent Accounts: For the primary "Parent" account (if any), this implementation initializes an entry in the
   * {@link RouteBroadcaster} and also configures the local routing-table appropriately. Note that other non-primary
   * parent accounts may have a static-route configured.
   * </li>
   * <li>Peer Accounts: For each `PEER` that participates in CCP, initializes an entry in the
   * {@link RouteBroadcaster}.</li>
   * <li>Child Accounts: At present, `CHILD` accounts do not participate in CCP (though this could easily
   * be changed in the future). For local routing decisions, all packets destined for a child account are forwarded to
   * an instance of {@link ChildAccountPaymentRouter}.</li>
   * <li>Static Routes: For any configured static route, this implementation updates the local routing table and also
   * attempts to register each associated account in the {@link RouteBroadcaster}.</li>
   * </ol>
   */
  private void initRoutingTables() {
    logger.debug("Entering #initRoutingTables...");

    localRoutingTable.reset();

    // TODO: No need to add a route for our own address because this is currently handled via Filter, but determine
    //  if this works correctly with CCP (e.g., we want to broadcast routes that are our children).
    // final InterledgerAddress ourAddress = this.connectorSettingsSupplier.get().operatorAddress();

    //////////////////
    // Parent Accounts
    //////////////////

    // Determine the default Route, and add it to the local routing table.
    this.determineDefaultRoute().ifPresent(defaultRoute -> {
          this.localRoutingTable.addRoute(defaultRoute);

          // Enable this Account for CCP (if appropriate)
          routeBroadcaster.registerCcpEnabledAccount(defaultRoute.nextHopAccountId());
        }
    );

    //////////////////
    // Peer Accounts
    //////////////////

    // All eligible PEER accounts are registered for CCP (if appropriate). Unless there is a static route configured,
    // then only CCP will populate routes amongst peers.
    accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER).stream()
        .forEach(routeBroadcaster::registerCcpEnabledAccount);

    //////////////////
    // Child Accounts
    //////////////////

    // Child accounts do not currently participate in CCP. If a static route is configured for a Child, it will be
    // populated below.

    //////////////////
    // Static Routes
    //////////////////

    // For any statically configured route...
    this.staticRoutesRepository.getAllStaticRoutes().stream().forEach(this::addStaticRoute);

    ////////////////////
    // Choose Best Paths
    ////////////////////

    // The above merely updates the local tables with any Routes we want to be proactively populated. This method
    // then takes every prefix and attempts to clarify all "best-path" choices.

    this.localRoutingTable.forEach((accountId, route) -> {
      // This method determines what the forwarding-tables should look like based upon "best path" algorithms.
      this.updatePrefix(route.routePrefix());
    });

  }

  private void addStaticRoute(StaticRoute staticRoute) {
    // ...attempt to register a CCP-enabled account (duplicate requests are fine).
    routeBroadcaster.registerCcpEnabledAccount(staticRoute.accountId());

    // This will add the prefix correctly _and_ update the forwarding table...
    updatePrefix(staticRoute.addressPrefix());
  }

  /**
   * Update all routing tables for the supplied {@code addressPrefix}.
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} to update details for.
   */
  @VisibleForTesting
  protected void updatePrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);

    final Optional<Route> newBestRoute = this.getCurrentBestPeerRouteForPrefix(addressPrefix);

    // If the local routing table changes, then update the forwarding routing table.
    if (this.updateLocalRoute(addressPrefix, newBestRoute)) {

      logger.info(
          "New BestRoute for Prefix `{}` is AccountId(`{}`)",
          addressPrefix.getValue(),
          newBestRoute.map(Route::nextHopAccountId).map(AccountId::value).orElse("n/a")
      );

      this.updateForwardingRoute(addressPrefix, newBestRoute);
    }
  }

  /**
   * A helper method to update the local routing table for {@code addressPrefix} with the optionally-specified {@code
   * newBestRoute}.
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} to update local routes for.
   * @param newBestRoute  An optionally-present new best route to use as the best local route for {@code
   *                      addressPrefix}.
   *
   * @return
   */
  private boolean updateLocalRoute(
      final InterledgerAddressPrefix addressPrefix, final Optional<Route> newBestRoute
  ) {
    Objects.requireNonNull(addressPrefix);
    Objects.requireNonNull(newBestRoute);

    final Optional<Route> currentBestRoute = this.localRoutingTable.getRouteByPrefix(addressPrefix);

    final Optional<AccountId> currentNextHop = currentBestRoute.map(Route::nextHopAccountId);
    final Optional<AccountId> newBestHop = newBestRoute.map(Route::nextHopAccountId);

    // If the current and next best hops are different, then update the routing tables...
    if (!currentNextHop.equals(newBestHop)) {
      newBestRoute
          .map(nbr -> {
            logger.debug(
                "New best route for prefix. prefix={} oldBest={} newBest={}",
                addressPrefix, currentNextHop, nbr.nextHopAccountId()
            );
            this.localRoutingTable.addRoute(nbr);
            return ROUTES_HAVE_CHANGED;
          })
          .orElseGet(() -> {
            logger.debug("No more routes available for prefix. prefix={}", addressPrefix);
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

    // Given an optionally-present newBestLocalRoute, compute the best next hop address...
    final Optional<AccountId> newBestNextHop = newBestLocalRoute
        // Map the Local Route to a
        .map(nblr -> {
          // Inject ourselves into the path that we'll send to remote peers...
          final List<InterledgerAddress> newPath = ImmutableList.<InterledgerAddress>builder()
              .add(connectorSettingsSupplier.get().operatorAddress())
              .addAll(nblr.path()).build();

          return ImmutableRoute.builder()
              .from(nblr)
              .path(newPath)
              // All Routes have an `auth` value that is derived from the prefix and a connector-wide secret that the
              // route-owner controls (e.g., this Connector for routes that come from us). When this route is advertised
              // via a forwarding table, the auth value is hashed again so that it is always unique from the perspective of
              // outside connectors (TODO: Verify this with https://github.com/interledger/rfcs/pull/455)
              .auth(Hashing.sha256().hashBytes(nblr.auth()).asBytes())
              .build();
        })
        .map(nblr -> {
          final InterledgerAddressPrefix globalPrefix = connectorSettingsSupplier.get().globalPrefix();
          final boolean hasGlobalPrefix = addressPrefix.getRootPrefix().equals(globalPrefix);
          final boolean isDefaultRoute = determineDefaultRoute()
              .map(Route::routePrefix)
              .map(routePrefix -> routePrefix.equals(nblr.routePrefix()))
              .orElse(false);

          // Don't advertise local customer routes that we originated. Packets for these destinations should still
          // reach us because we are advertising our own address as a prefix.
          final boolean isLocalCustomerRoute = addressPrefix.getValue()
              .startsWith(this.connectorSettingsSupplier.get().operatorAddress().getValue()) &&
              nblr.path().size() == 1;

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
        .map(Route::nextHopAccountId);

    // Only if there's a newBestNextHop, update the forwarding tables...
    newBestNextHop.ifPresent(nbnh -> {

      // Don't look in the log, but look in the actual Routing table for the prefix...
      final Optional<RouteUpdate> currentBest = this.outgoingRoutingTable.getRouteByPrefix(addressPrefix);
      final Optional<AccountId> currentBestNextHop = currentBest
          .map(RouteUpdate::route)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Route::nextHopAccountId);

      // There's a new NextBestHop, so update the forwarding table, but only if it's different from the optionally
      // present currentBestNextHop.
      if (!currentBestNextHop.isPresent() || !currentBestNextHop.get().equals(nbnh)) {
        final int newEpoch = this.outgoingRoutingTable.getCurrentEpoch();

        final RouteUpdate newBestRouteUpdate = ImmutableRouteUpdate.builder()
            .routePrefix(addressPrefix)
            .route(newBestLocalRoute)
            .epoch(newEpoch)
            .build();

        // Add the route update to the outgoing routing table so that it will be sent to remote peers on the next CCP
        // send operation.
        this.outgoingRoutingTable.addRoute(newBestRouteUpdate);
        logger.debug("Logging route update. update={}", newBestRouteUpdate);

        // If there's a current-best, null-out the epoch.
        currentBest.ifPresent(ru -> outgoingRoutingTable.resetEpochValue(ru.epoch()));

        // Set the new best into the new epoch.
        outgoingRoutingTable.setEpochValue(newEpoch, newBestRouteUpdate);

        // Update the epoch after updating the log so that the Route forwarder doesn't think there's new things to
        // send before they're actually added.
        //outgoingRoutingTable.compareAndSetCurrentEpoch(this.outgoingRoutingTable.getCurrentEpoch(), newEpoch);

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
              this.outgoingRoutingTable.getRouteByPrefix(subPrefix)
                  .ifPresent(routeUpdate -> this.updateForwardingRoute(subPrefix, routeUpdate.route()));
            });
      }
    });
  }

  /**
   * Returns the current "best route" for the specified prefix according to the following rules:
   *
   * <ol>
   * <li>If a static-route exists for a prefix, then that is always the "best route"</li>.
   * <li>Otherwise, </li>
   * </ol>. The rules for this
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} to find the best peer route for.
   *
   * @return An optionally-present {@link Route}.
   */
  @VisibleForTesting
  protected Optional<Route> getCurrentBestPeerRouteForPrefix(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);

    // Static-routes have highest priority...
    return Optional.ofNullable(
        staticRoutesRepository.getAllStaticRoutes().stream()
            .filter(staticRoute -> staticRoute.addressPrefix().equals(addressPrefix))
            .findFirst()
            .map(staticRoute -> {
              // If there's a static route, then use it, even if the account doesn't exist. In this
              // way, if the account does eventually come into existence, then things will work properly. If the account never
              // comes into existence, then the Router and/or Link will simply reject if anything is attempted.
              return (Route) ImmutableRoute.builder()
                  .routePrefix(addressPrefix)
                  .nextHopAccountId(staticRoute.accountId())
                  .auth(this.constructRouteAuth(addressPrefix))
                  .build();
            })
            .orElseGet(() -> {
                  //...then look in the receiver.
                  // If we get here, there was no statically-configured route, _or, there was a statically configured route
                  // but no account existed. Either way, look for a local route.
                  return localRoutingTable.getRouteByPrefix(addressPrefix)
                      .orElseGet(() -> {
                        // If we get here, there was no local route, so search through all tracked accounts and sort all
                        // of the routes to find the shortest-path (i.e., lowest weight) route that will work for
                        // `addressPrefix`. This is the best route.

                        return this.routeBroadcaster.getAllCcpEnabledAccounts()
                            .map(RoutableAccount::ccpReceiver)
                            .map(ccpReceiver -> ccpReceiver.getIncomingRouteForPrefix(addressPrefix))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .sorted(routingTableEntryComparator)
                            .collect(Collectors.<IncomingRoute>toList()).stream()
                            .findFirst()
                            .map(bestRoute -> (Route) ImmutableRoute.builder()
                                .routePrefix(bestRoute.routePrefix())
                                .nextHopAccountId(bestRoute.peerAccountId())
                                .path(bestRoute.path())
                                .auth(bestRoute.auth())
                                .build()
                            )
                            .orElse(null);
                      });
                }
            )
    );
  }

  /**
   * Constructs the value of {@link Route#auth()} by HMAC'ing {@code addressPrefix} using a routing-secret configured
   * for this Connector.
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} to find the best peer route for.
   *
   * @return A byte array containing the route auth.
   */
  private byte[] constructRouteAuth(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix);

    // Decrypt the routingSecret, but only momentarily...
    final byte[] routingSecret = decryptor.decrypt(EncryptedSecret.fromEncodedValue(
        connectorSettingsSupplier.get().globalRoutingSettings().routingSecret()
    ));

    try {
      return HMAC(routingSecret, addressPrefix);
    } finally {
      // Zero-out all bytes in the `sharedSecretBytes` array.
      Arrays.fill(routingSecret, (byte) 0);
    }
  }

  /**
   * Determine the default route for this connector, if any.
   */
  private Optional<Route> determineDefaultRoute() {
    final Optional<AccountId> nextHopForDefaultRoute;
    if (connectorSettingsSupplier.get().globalRoutingSettings().isUseParentForDefaultRoute()) {
      nextHopForDefaultRoute =
          this.accountSettingsRepository.findFirstByAccountRelationshipWithConversion(AccountRelationship.PARENT)
              .map(AccountSettings::accountId)
              .map(Optional::of)
              .orElseThrow(() -> new RuntimeException(
                  "Connector was configured to use a Parent account as the nextHop for the default route, but no Parent "
                      +
                      "Account was configured!"
              ));
    } else if (connectorSettingsSupplier.get().globalRoutingSettings().defaultRoute().isPresent()) {
      nextHopForDefaultRoute = connectorSettingsSupplier.get().globalRoutingSettings().defaultRoute()
          .map(Optional::of)
          .orElseThrow(() -> new RuntimeException("Connector was configured to use a default address as the nextHop " +
              "for the default route, but no Account was configured for this address!"));
    } else {
      if (numDefaultRouteWarnings++ <= 0) {
        logger.warn(
            "No Default Route configured (A default route provides a fallback upstream link for any packets that are not"
                +
                " intrinsically routable)."
        );
      }
      nextHopForDefaultRoute = Optional.empty();
    }

    // Emit the default route.
    nextHopForDefaultRoute.ifPresent(defaultRoute -> logger.info("Default Route Configured: " + defaultRoute));

    final InterledgerAddressPrefix globalPrefix = connectorSettingsSupplier.get().globalPrefix();
    final Optional<Route> defaultRoute = nextHopForDefaultRoute.map(nextHopAccountId ->
        ImmutableRoute.builder()
            .routePrefix(globalPrefix)
            .nextHopAccountId(nextHopAccountId)
            // empty path.
            // never expires
            .auth(this.constructRouteAuth(globalPrefix))
            .build()
    );

    // Emit the default route.
    defaultRoute.ifPresent($ -> logger.info("Default Route Configured: " + $));
    return defaultRoute;
  }

}
