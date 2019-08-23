package org.interledger.connector.routing;

import com.google.common.collect.Maps;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <p>Centralizes all Route broadcasting logic in order to coordinate routing updates across peer-account
 * connections.</p>
 *
 * <p>This implementation tracks a Collection of routable accounts, which are simply accounts that are eligible
 * for sending or receiving (or both) CCP route updates. It should be noted that this class does not track any sort of
 * ILP address mapping to a particular {@link RoutableAccount}. Instead, this class merely allows a given account to
 * hold a CCP Sender/Receiver that can be used to process CCP message.</p>
 */
public class DefaultRouteBroadcaster implements RouteBroadcaster {
  private static final boolean SHOULD_NOT_SEND_ROUTES = false;
  private static final boolean SHOULD_NOT_RECEIVE_ROUTES = false;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  private final CodecContext ccpCodecContext;

  // Master outgoing routing table, used for routes that this connector broadcasts to peer accounts.
  private final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  // A Map of accounts -> RoutableAccounts that currently have CCP sender/receivers running.
  private final Map<AccountId, RoutableAccount> ccpEnabledAccounts;

  private final AccountSettingsRepository accountSettingsRepository;
  private final LinkManager linkManager;

  /**
   * Required-args Constructor.
   */
  public DefaultRouteBroadcaster(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final CodecContext ccpCodecContext,
    final ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkManager linkManager
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.ccpCodecContext = Objects.requireNonNull(ccpCodecContext);
    this.outgoingRoutingTable = Objects.requireNonNull(outgoingRoutingTable);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.linkManager = Objects.requireNonNull(linkManager);

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

    final AccountId accountId = accountSettings.getAccountId();
    final boolean sendRoutes = this.shouldSendRoutes(accountSettings);
    final boolean receiveRoutes = this.shouldReceiveRoutes(accountSettings);
    if (!sendRoutes && !receiveRoutes) {
      logger.warn("Not sending nor receiving routes for peer. accountId={}", accountId);
      logger.debug("Checking to see if Account {} has a static-route...", accountId);
      return Optional.empty();
    } else {

      final RoutableAccount routableAccountForPeer = Optional.ofNullable(this.ccpEnabledAccounts.get(accountId))
        .map(existingPeer -> {
          // Every time we reconnect, we'll send a new route control message to make sure they are still sending us
          // routes, but only as long as receiving is enabled.
          if (receiveRoutes) {
            existingPeer.getCcpReceiver().sendRouteControl();
          }
          logger.warn("CCP Peer already registered with RouteBroadcaster using AccountId=`{}`", accountId);
          return existingPeer;
        })
        .orElseGet(() -> {
          // The Account in question did not have an existing link in this routing service, so create a new link and
          // initialize it.
          final Link<?> link = linkManager.getOrCreateLink(accountId);
          logger.info(
            "Adding Link to ccpEnabledAccounts. accountId={} sendRoutes={} isReceiveRoutes={}",
            accountId, sendRoutes, receiveRoutes
          );
          final RoutableAccount newPeerAccount = ImmutableRoutableAccount.builder()
            .accountId(accountId)
            .ccpSender(constructCcpSender(accountSettings.getAccountId(), link))
            .ccpReceiver(constructCcpReceiver(accountSettings.getAccountId(), link))
            .build();

          this.setCcpEnabledAccount(newPeerAccount);

          // Always send a new RoutControl request to the remote peer, but only if it's connected.
          newPeerAccount.getCcpReceiver().sendRouteControl();

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

  /**
   * Determines if the link configured for the accountSettings in {@code accountSettings} should send routes to the
   * remote peer accountSettings.
   *
   * @param accountSettings An instance of {@link AccountSettings} for a remote peer accountSettings.
   *
   * @return {@code true} if the link is configured to send routes, {@code false} otherwise.
   */
  private boolean shouldSendRoutes(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    if (accountSettings.isChildAccount()) {
      return SHOULD_NOT_SEND_ROUTES;
    } else {
      return accountSettings.isSendRoutes();
    }
  }

  /**
   * Determines if the link configured for the accountSettings in {@code accountSettings} should receive routes from the
   * remote peer accountSettings.
   *
   * @param accountSettings An instance of {@link AccountSettings} for a remote peer accountSettings.
   *
   * @return {@code true} if the link is configured to receive routes, {@code false} otherwise.
   */
  private boolean shouldReceiveRoutes(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    if (accountSettings.isChildAccount()) {
      return SHOULD_NOT_RECEIVE_ROUTES;
    } else {
      return accountSettings.isReceiveRoutes();
    }
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

    if (this.ccpEnabledAccounts.putIfAbsent(routableAccount.getAccountId(), routableAccount) != null) {
      throw new RuntimeException(
        String.format("AccountId `%s` existed in the RouteBroadcaster already.", routableAccount.getAccountId())
      );
    }
  }

  // TODO: Remove this? CCP Senders/Receivers should be able to be turned off via config.
  protected void removeAccount(final AccountId accountId) {
    Optional.ofNullable(this.ccpEnabledAccounts.get(accountId))
      .ifPresent(peer -> {
        logger.trace("Remove peer. peerId={}", accountId);

        // Stop the CcpSender from broadcasting routes...
        peer.getCcpSender().stopBroadcasting();

        // We have to removeEntry the peer before calling updatePrefix on each of its advertised prefixes in order to
        // find the next best route.
        this.ccpEnabledAccounts.remove(accountId);
      });
  }
}
