package com.sappenin.interledger.ilpv4.connector.links.loopback;

import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactory;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerAddress;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for creating Links that can handle the `Loopback` packets.
 */
public class LoopbackLinkFactory implements LinkFactory {

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(linkSettings);

    final Link<?> link;
    switch (linkSettings.getLinkType().value()) {
      case LoopbackLink.LINK_TYPE_STRING: {
        link = new LoopbackLink(operatorAddressSupplier, linkSettings);
        break;
      }
      default: {
        throw new RuntimeException(String.format("Invalid LinkType: %s", linkSettings.getLinkType()));
      }
    }

    return link;

  }

  @Override
  public boolean supports(LinkType linkType) {
    return LoopbackLink.LINK_TYPE.equals(linkType);
  }

}
