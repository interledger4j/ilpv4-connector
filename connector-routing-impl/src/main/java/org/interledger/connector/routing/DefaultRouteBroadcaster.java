package org.interledger.connector.routing;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <p>Centralizes all Route broadcasting logic in order to coordinate routing updates across peer-account
 * connections.</p>
 *
 * <p>This implementation tracks a Collection of routable accounts, which are simply accounts that are eligible
 * for sending or receiving (or both) CCP route updates. It should be noted that this class does not track any sort of
 * ILP address mapping to a particular {@link RoutableAccount}. Instead, this class merely allows a given account to
 * hold a CCP Sender/Receiver that can be used to process CCP messages.</p>
 */
public class DefaultRouteBroadcaster implements RouteBroadcaster {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final CodecContext ccpCodecContext;

  // Master outgoing routing table, used for routes that this connector broadcasts to peer accounts.
  private final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  // A Map of accounts -> RoutableAccounts that currently have CCP sender/receivers running.
  private final Map<AccountId, RoutableAccount> ccpEnabledAccounts;

  private final AccountSettingsRepository accountSettingsRepository;
  private final LinkManager linkManager;

  // Use the executorService to run this task outside of the main thread. In the case where we have _many_
  // receivers (e.g., many peer/child accounts), we don't want a single thread-per-receiver. Instead, we use
  // a single threadpool for all routing.
  private final ExecutorService executorService;

  /**
   * Required-args Constructor.
   */
  public DefaultRouteBroadcaster(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final CodecContext ccpCodecContext,
    final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkManager linkManager,
    final ExecutorService executorService
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.outgoingRoutingTable = Objects.requireNonNull(outgoingRoutingTable);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.executorService = Objects.requireNonNull(executorService);

    this.ccpEnabledAccounts = Maps.newConcurrentMap();
  }

  @Override
  public Optional<RoutableAccount> registerCcpEnabledAccount(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return accountSettingsRepository.findByAccountIdWithConversion(accountId)
      .map(this::registerCcpEnabledAccount)
      .filter(Optional::isPresent)
      .map(Optional::get);
  }

  @Override
  public Optional<RoutableAccount> registerCcpEnabledAccount(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    final AccountId accountId = accountSettings.accountId();

    // The account described by accountSettings sends routes to its peer (e.g., a `CHILD` or `PEER` account)
    final boolean sendRoutes = accountSettings.isSendRoutes();
    // The account described by accountSettings receives routes from its peer (e.g., a `PARENT` or `PEER` account)
    final boolean receiveRoutes = accountSettings.isReceiveRoutes();
    if (!sendRoutes && !receiveRoutes) {
      logger.warn("Not sending nor receiving routes for peer. accountId={}", accountId);
      return Optional.empty();
    } else {

      final RoutableAccount routableAccountForPeer = Optional.ofNullable(this.ccpEnabledAccounts.get(accountId))
        .map(existingPeer -> {
          // Every time we reconnect, we'll send a new route control message to make sure they are still sending us
          // routes, but only as long as receiving is enabled.
          if (receiveRoutes) {
            // Use the executorService to run this task outside of the main thread. In the case where we have _many_
            // receivers (e.g., many peer/child accounts, we don't want a single thread-per-receiver. Instead, we use
            // a single threadpool for all routing.
            this.executorService.submit(() -> existingPeer.ccpReceiver().sendRouteControl());
          }
          logger.warn("CCP Peer already registered with RouteBroadcaster using AccountId=`{}`", accountId);
          return existingPeer;
        })
        .orElseGet(() -> {
          // The Account in question did not have an existing link in this routing service, so create a new link and
          // initialize it.
          final Link<?> link = linkManager.getOrCreateLink(accountId);
          logger.info(
            "Adding Link to ccpEnabledAccounts: accountId={} sendRoutes={} receiveRoutes={}",
            accountId, sendRoutes, receiveRoutes
          );
          final RoutableAccount newPeerAccount = ImmutableRoutableAccount.builder()
            .accountId(accountId)
            .ccpSender(constructCcpSender(accountSettings.accountId(), link))
            .ccpReceiver(constructCcpReceiver(accountSettings.accountId(), link))
            .build();

          this.setCcpEnabledAccount(newPeerAccount);

          // Send a RoutControl request to the remote peer, but only if this Connector should receive routes from
          // that peer.
          if (receiveRoutes) {
            // Use the executorService to run this task outside of the main thread. In the case where we have _many_
            // receivers (e.g., many peer/child accounts, we don't want a single thread-per-receiver. Instead, we use
            // a single threadpool for all routing.
            this.executorService.submit(() -> newPeerAccount.ccpReceiver().sendRouteControl());
          }

          return newPeerAccount;
        });
      return Optional.of(routableAccountForPeer);
    }
  }

  @Override
  public Optional<RoutableAccount> getCcpEnabledAccount(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return Optional.ofNullable(this.ccpEnabledAccounts.get(accountId));
  }

  @Override
  public Stream<RoutableAccount> getAllCcpEnabledAccounts() {
    return this.ccpEnabledAccounts.values().stream();
  }

  private CcpSender constructCcpSender(final AccountId peerAccountId, final Link link) {
    Objects.requireNonNull(peerAccountId);
    Objects.requireNonNull(link);
    return new DefaultCcpSender(
      connectorSettingsSupplier, peerAccountId, link, outgoingRoutingTable, accountSettingsRepository, ccpCodecContext
    );
  }

  private CcpReceiver constructCcpReceiver(final AccountId peerAccountId, final Link link) {
    Objects.requireNonNull(peerAccountId);
    Objects.requireNonNull(link);
    return new DefaultCcpReceiver(connectorSettingsSupplier, peerAccountId, link, ccpCodecContext);
  }

  /**
   * Helper method to add a {@link RoutableAccount} into the Collection of accounts tracked by this implementation.
   */
  private void setCcpEnabledAccount(final RoutableAccount routableAccount) {
    Objects.requireNonNull(routableAccount);

    if (this.ccpEnabledAccounts.putIfAbsent(routableAccount.accountId(), routableAccount) != null) {
      throw new RuntimeException(
        String.format("AccountId `%s` existed in the RouteBroadcaster already.", routableAccount.accountId())
      );
    }
  }

  // TODO: Remove this? CCP Senders/Receivers should be able to be turned off via config.
  protected void removeAccount(final AccountId accountId) {
    Optional.ofNullable(this.ccpEnabledAccounts.get(accountId))
      .ifPresent(peer -> {
        logger.trace("Remove peer. peerId={}", accountId);

        // Stop the CcpSender from broadcasting routes...
        peer.ccpSender().stopBroadcasting();

        // We have to removeEntry the peer before calling updatePrefix on each of its advertised prefixes in order to
        // find the next best route.
        this.ccpEnabledAccounts.remove(accountId);
      });
  }
}
