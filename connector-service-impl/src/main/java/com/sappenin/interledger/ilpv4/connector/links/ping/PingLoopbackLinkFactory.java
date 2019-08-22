package com.sappenin.interledger.ilpv4.connector.links.ping;

import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactory;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.core.InterledgerAddress;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for creating Links that can handle the `Unidirectional Ping` packets.
 */
public class PingLoopbackLinkFactory implements LinkFactory {

  private final LinkEventEmitter linkEventEmitter;
  private PingLoopbackLink lazyPingLoopbackLink;

  /**
   * Required-args Constructor.
   */
  public PingLoopbackLinkFactory(final LinkEventEmitter linkEventEmitter) {
    this.linkEventEmitter = Objects.requireNonNull(linkEventEmitter);
  }

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(linkSettings);

    if (!this.supports(linkSettings.getLinkType())) {
      throw new RuntimeException(
        String.format("LinkType `%s` not supported by this factory!", linkSettings.getLinkType())
      );
    }

    if (lazyPingLoopbackLink != null) {
      return lazyPingLoopbackLink;
    } else {
      synchronized (this) {
        if (lazyPingLoopbackLink == null) {
          lazyPingLoopbackLink = new PingLoopbackLink(operatorAddressSupplier, linkSettings, linkEventEmitter);
        }
        return lazyPingLoopbackLink;
      }
    }
  }

  @Override
  public boolean supports(LinkType linkType) {
    return PingLoopbackLink.LINK_TYPE.equals(linkType);
  }

}
