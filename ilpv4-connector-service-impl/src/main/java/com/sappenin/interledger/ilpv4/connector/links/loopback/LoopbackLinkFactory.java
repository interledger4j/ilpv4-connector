package com.sappenin.interledger.ilpv4.connector.links.loopback;

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
 * An implementation of {@link LinkFactory} for creating Links that can handle the `Loopback` packets.
 */
public class LoopbackLinkFactory implements LinkFactory {

  private final LinkEventEmitter linkEventEmitter;

  public LoopbackLinkFactory(final LinkEventEmitter linkEventEmitter) {
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

    return new LoopbackLink(operatorAddressSupplier, linkSettings, linkEventEmitter);
  }

  @Override
  public boolean supports(LinkType linkType) {
    return LoopbackLink.LINK_TYPE.equals(linkType);
  }

}
