package com.sappenin.interledger.ilpv4.connector.links.ping;

import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkFactory;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.encoding.asn.framework.CodecContext;

import java.util.Objects;

/**
 * An implementation of {@link LinkFactory} for creating Links that can handle the `Ping` protocol.
 */
public class PingProtocolLinkFactory implements LinkFactory {

  private final CodecContext ilpCodecContext;

  public PingProtocolLinkFactory(final CodecContext ilpCodecContext) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
  }

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(final LinkSettings linkSettings) {
    Objects.requireNonNull(linkSettings);

    final Link<?> link;
    switch (linkSettings.getLinkType().value()) {
      case PingProtocolLink.LINK_TYPE_STRING: {
        link = new PingProtocolLink(linkSettings, ilpCodecContext);
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
    return PingProtocolLink.LINK_TYPE.equals(linkType);
  }

}