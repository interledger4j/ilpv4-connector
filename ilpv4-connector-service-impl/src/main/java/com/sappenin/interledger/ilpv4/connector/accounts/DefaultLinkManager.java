package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactoryProvider;
import org.interledger.connector.link.LinkId;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.connector.link.events.LinkDisconnectedEvent;
import org.interledger.connector.link.events.LinkErrorEvent;
import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.core.InterledgerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A default implementation of {@link LinkManager} that stores all links in-memory.
 */
public class DefaultLinkManager implements LinkManager, LinkEventListener {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  private final EventBus eventBus;
  private final LinkFactoryProvider linkFactoryProvider;

  /**
   * Required-args constructor.
   */
  public DefaultLinkManager(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final EventBus eventBus,
    final LinkFactoryProvider linkFactoryProvider
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.eventBus = Objects.requireNonNull(eventBus);
    this.eventBus.register(this);
    this.linkFactoryProvider = Objects.requireNonNull(linkFactoryProvider);
  }

  public Link<?> createLink(final AccountId accountId, final LinkSettings linkSettings) {
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(linkSettings);

    //Use the first linkFactory that supports the linkType...
    final Link<?> link = this.linkFactoryProvider.getLinkFactory(linkSettings.getLinkType())
      .map(linkFactory -> linkFactory.constructLink(operatorAddressSupplier, linkSettings))
      .orElseThrow(() -> new RuntimeException(
        String.format("No registered LinkFactory supports: %s", linkSettings.getLinkType()))
      );

    // TODO: Refactor Link to require a linkId. We shouldn't construct a link (client or server) if we don't
    //  know the Account, which is the LinkId.

    // Set the LinkId to match the AccountId...this way the Link can always use this value to represent the
    // accountId that a given link should use.
    ((AbstractLink) link).setLinkId(LinkId.of(accountId.value()));

    return link;
  }

  @Override
  // No need to @Subscribe
  public void onConnect(LinkConnectedEvent event) {
    // No-op.
  }

  @Override
  // No need to @Subscribe
  public void onDisconnect(LinkDisconnectedEvent event) {
    // No-op.
  }

  @Override
  @Subscribe
  public void onError(LinkErrorEvent event) {
    Objects.requireNonNull(event);
    logger.error("Link: {}; LinkError: {}", event.getLink(), event.getError());
  }
}
