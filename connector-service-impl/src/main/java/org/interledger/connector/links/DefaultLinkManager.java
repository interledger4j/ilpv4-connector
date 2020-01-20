package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountIdResolver;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.CircuitBreakingLink;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.StatefulLink;
import org.interledger.link.events.LinkConnectedEvent;
import org.interledger.link.events.LinkConnectionEventListener;
import org.interledger.link.events.LinkDisconnectedEvent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A default implementation of {@link LinkManager} that stores all connectedLinks in-memory.
 *
 * TODO: Consider making this Manager stateless. See https://github.com/interledger4j/ilpv4-connector/issues/290 for
 * more details.
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultLinkManager implements LinkManager, LinkConnectionEventListener {

  private final AccountIdResolver accountIdResolver;
  // Note that HTTP links don't "connect" per-se (meaning they don't strictly need to be tracked) but Websocket
  // client-links _do_ need to be tracked so we know if a remote client has connected to us (this manager won't be
  // able to initiate that type of connection).
  private final Map<AccountId, Link<? extends LinkSettings>> connectedLinks = Maps.newConcurrentMap();

  private final Supplier<InterledgerAddress> operatorAddressSupplier;

  // NOTE: Note using the AccountSettings cache because Links are typically created infrequently, and it's better to
  // have the most up-to-date data here.
  private final AccountSettingsRepository accountSettingsRepository;
  private final LinkSettingsFactory linkSettingsFactory;
  private final LinkFactoryProvider linkFactoryProvider;
  private final CircuitBreakerConfig defaultCircuitBreakerConfig;
  private final Link<?> pingLink;

  /**
   * Required-args constructor.
   */
  public DefaultLinkManager(
    final Supplier<InterledgerAddress> operatorAddressSupplier,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkSettingsFactory linkSettingsFactory,
    final LinkFactoryProvider linkFactoryProvider,
    final AccountIdResolver accountIdResolver,
    final CircuitBreakerConfig defaultCircuitBreakerConfig,
    final EventBus eventBus
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.linkSettingsFactory = Objects.requireNonNull(linkSettingsFactory);
    this.linkFactoryProvider = Objects.requireNonNull(linkFactoryProvider);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig);

    Objects.requireNonNull(eventBus).register(this);

    this.pingLink = linkFactoryProvider.getLinkFactory(PingLoopbackLink.LINK_TYPE)
      .constructLink(
        operatorAddressSupplier,
        LinkSettings.builder()
          .linkType(PingLoopbackLink.LINK_TYPE)
          .build()
      );
  }

  // Required for efficient Link-lookup-by-AccountId in the PacketSwitch.
  @Override
  public Link<? extends LinkSettings> getOrCreateLink(final AccountId accountId) {
    Objects.requireNonNull(accountId);
    return Optional.ofNullable(this.connectedLinks.get(accountId))
      .orElseGet(() -> {
        // Convert to LinkSettings...
        final AccountSettings accountSettings = accountSettingsRepository.findByAccountIdWithConversion(accountId)
          .orElseThrow(() -> new AccountNotFoundProblem(accountId));

        return (Link) getOrCreateLink(accountSettings);
      });
  }

  @Override
  public Link<? extends LinkSettings> getOrCreateLink(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    final AccountId accountId = accountSettings.accountId();
    return Optional.ofNullable(this.connectedLinks.get(accountId))
      .orElseGet(() -> {
        // Convert to LinkSettings...
        final LinkSettings linkSettings = linkSettingsFactory.construct(accountSettings);
        return createLink(accountId, linkSettings);
      });
  }

  @Override
  public Link<? extends LinkSettings> getOrCreateLink(final AccountId accountId, final LinkSettings linkSettings) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(linkSettings);

    return Optional.ofNullable(this.connectedLinks.get(accountId))
      .orElseGet(() -> createLink(accountId, linkSettings));
  }

  @VisibleForTesting
  protected Link createLink(final AccountId accountId, final LinkSettings linkSettings) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(linkSettings);

    //Use the first linkFactory that supports the linkType...
    final Link<?> link = this.linkFactoryProvider
      .getLinkFactory(linkSettings.getLinkType())
      .constructLink(operatorAddressSupplier, linkSettings);

    // Set the LinkId to match the AccountId...this way the Link can always use this value to represent the
    // accountId that a given link should use.
    link.setLinkId(LinkId.of(accountId.value()));

    // Wrap the Link in a CircuitBreaker.
    final CircuitBreakingLink circuitBreakingLink = new CircuitBreakingLink(link, defaultCircuitBreakerConfig);

    if (link instanceof StatefulLink) {
      StatefulLink statefulLink = ((StatefulLink) link);
      // Register this Manager as a LinkEvent Listener...
      statefulLink.addLinkEventListener(this);
      // Connect the link...
      statefulLink.connect().join();
    }

    return circuitBreakingLink;
  }

  @Override
  public Set<Link<? extends LinkSettings>> getAllConnectedLinks() {
    return new HashSet<>(this.connectedLinks.values());
  }

  /**
   * Accessor for the {@link Link} that processes Ping protocol requests.
   *
   * @return A {@link Link} for processing unidirectional and bidirectional ping requests.
   */
  @Override
  public Link<? extends LinkSettings> getPingLink() {
    return pingLink;
  }

  ////////////////////////
  // Link Event Listener
  ////////////////////////

  /**
   * When a Link connects, it becomes tracked in this manager so that other services can access a Link for a given
   * account.
   */
  @Override
  @Subscribe
  public void onConnect(final LinkConnectedEvent event) {
    Objects.requireNonNull(event);
    final AccountId accountId = accountIdResolver.resolveAccountId(event.getLink());
    this.connectedLinks.put(accountId, event.getLink());
  }

  /**
   * There are two types of accounts: pre-configured, and dynamic. Preconfigured accounts should never be removed from
   * the account manager when they disconnect. However, in the case of dynamic accounts (i.e., accounts that get created
   * dynamically in response to an incoming connection), these will take up memory and should not continue to be tracked
   * if no connection is currently open.
   */
  @Override
  @Subscribe
  public void onDisconnect(final LinkDisconnectedEvent event) {
    Objects.requireNonNull(event);

    final AccountId accountId = this.accountIdResolver.resolveAccountId(event.getLink());
    // Remove the Link from the Set of connected links for the specified account.
    this.connectedLinks.remove(accountId);
  }

}
