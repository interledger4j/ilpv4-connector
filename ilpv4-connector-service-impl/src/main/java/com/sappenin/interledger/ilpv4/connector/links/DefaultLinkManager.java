package com.sappenin.interledger.ilpv4.connector.links;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.LinkManager;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.CircuitBreakingLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.LinkId;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.connector.link.events.LinkDisconnectedEvent;
import org.interledger.connector.link.events.LinkErrorEvent;
import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.core.InterledgerAddress;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A default implementation of {@link LinkManager} that stores all connectedLinks in-memory.
 */
public class DefaultLinkManager implements LinkManager, LinkEventListener {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final AccountIdResolver accountIdResolver;
  // Note that HTTP links don't "connect" per-se (meaning they don't strictly need to be tracked) but Websocket
  // client-links _do_ need to be tracked so we know if a remote client has connected to us (this manager won't be
  // able to initiate that type of connection).
  private final Map<AccountId, Link<? extends LinkSettings>> connectedLinks = Maps.newConcurrentMap();

  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  private final LinkFactoryProvider linkFactoryProvider;
  private final LinkSettingsFactory linkSettingsFactory;
  private final AccountSettingsRepository accountSettingsRepository;

  private final CircuitBreakerConfig defaultCircuitBreakerConfig;

  /**
   * Required-args constructor.
   */
  public DefaultLinkManager(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final LinkSettingsFactory linkSettingsFactory,
    final LinkFactoryProvider linkFactoryProvider,
    final AccountIdResolver accountIdResolver,
    AccountSettingsRepository accountSettingsRepository, final CircuitBreakerConfig defaultCircuitBreakerConfig,
    final EventBus eventBus
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.linkSettingsFactory = Objects.requireNonNull(linkSettingsFactory);
    this.linkFactoryProvider = Objects.requireNonNull(linkFactoryProvider);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig);
    Objects.requireNonNull(eventBus).register(this);
  }

  @Override
  public Link<? extends LinkSettings> getOrCreateLink(final AccountId accountId) {
    return Optional.ofNullable(this.connectedLinks.get(accountId))
      .orElseGet(() -> {
        final AccountSettings accountSettings = accountSettingsRepository.safeFindByAccountId(accountId);
        return this.getOrCreateLink(accountSettings);
      });
  }

  @Override
  public Link<? extends LinkSettings> getOrCreateLink(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    final LinkSettings linkSettings = linkSettingsFactory.construct(accountSettings);
    return this.getOrCreateLink(accountSettings.getAccountId(), linkSettings);
  }

  @Override
  public Link<? extends LinkSettings> getOrCreateLink(final AccountId accountId, final LinkSettings linkSettings) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(linkSettings);

    //Use the first linkFactory that supports the linkType...
    final Link<?> link = this.linkFactoryProvider.getLinkFactory(linkSettings.getLinkType())
      .map(linkFactory -> linkFactory.constructLink(operatorAddressSupplier, linkSettings))
      .orElseThrow(() -> new RuntimeException(
        String.format("No registered LinkFactory supports: %s", linkSettings.getLinkType()))
      );

    // Set the LinkId to match the AccountId...this way the Link can always use this value to represent the
    // accountId that a given link should use.
    ((AbstractLink) link).setLinkId(LinkId.of(accountId.value()));

    // Register this Manager as a LinkEvent Listener...
    link.addLinkEventListener(this);

    // TODO: Once Issue https://github.com/sappenin/java-ilpv4-connector/issues/64 is fixed, this should be
    //  configurable from the account settings, which in this case should propagate to the link settings.
    // Wrap the Link in a CircuitBreaker.
    final CircuitBreakingLink circuitBreakingLink = new CircuitBreakingLink(link, defaultCircuitBreakerConfig);
    try {
      circuitBreakingLink.connect().get();
      return circuitBreakingLink;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Set<Link<?>> getAllConnectedLinks() {
    return this.connectedLinks.values().stream().collect(Collectors.toSet());
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

  @Override
  @Subscribe
  public void onError(final LinkErrorEvent event) {
    Objects.requireNonNull(event);
    logger.error("Link: {}; LinkError: {}", event.getLink(), event.getError());
  }
}
