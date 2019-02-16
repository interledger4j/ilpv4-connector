package com.sappenin.interledger.ilpv4.connector.links.peer;

import com.sappenin.interledger.ilpv4.connector.links.InternallyRoutedLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;

import java.util.Optional;

/**
 * <p>A LPIv2 {@link Link} that implements the ILP Peer Config sub-protocol, which allows a Connector to support
 * IL-DCP in order to provide a child-link an Interledger address rooted in this Connector's namespace.</p>
 *
 * <p>This link functions like the `ildcpHostController` in the Javascript implementation.</p>
 */
public class PeerConfigProtocolLink extends InternallyRoutedLink implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "PEER_CONFIG_LINK";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  /**
   * Required-args Constructor.
   *
   * @param linkSettings    A {@link LinkSettings} that specifies all link options.
   * @param oerCodecContext
   */
  public PeerConfigProtocolLink(final LinkSettings linkSettings, final CodecContext oerCodecContext) {
    super(linkSettings, oerCodecContext);
  }

  @Override
  public Optional<InterledgerResponsePacket> sendPacket(InterledgerPreparePacket preparePacket) {
    // TODO
    throw new RuntimeException("FIXME!");
  }
}
