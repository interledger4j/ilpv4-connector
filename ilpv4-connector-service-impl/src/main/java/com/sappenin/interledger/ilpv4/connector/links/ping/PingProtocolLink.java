package com.sappenin.interledger.ilpv4.connector.links.ping;

import com.sappenin.interledger.ilpv4.connector.links.InternallyRoutedLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;

import java.util.Objects;

/**
 * <p>A {@link Link} that implements the <tt>PING</tt> protocol. The packet-switch will forward all incoming packets
 * addressed to this connector's operating address to this link (via an appropriate routing table entry). Once this link
 * is called to process a packet, this link will merely respond with fulfill packets containing a known response, and
 * will also track the balance of payments this connector has accrued due to incoming ping-payments.</p>
 *
 * <p>Note that while this link is tracking payments related to ping requests, this account is actually between the
 * connector and itself, so unless it makes sense for tracking purposes, settling the account balance tracked by this
 * link is unnecessary.</p>
 *
 * TODO: Add RFC link.
 *
 * @see ""
 */
public class PingProtocolLink extends InternallyRoutedLink implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "PING_PROTOCOL";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = PING_PROTOCOL_FULFILLMENT.getCondition();

  /**
   * Required-args constructor.
   */
  public PingProtocolLink(final LinkSettings linkSettings, final CodecContext oerCodecContext) {
    super(linkSettings, oerCodecContext);
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    if (preparePacket.getExecutionCondition().equals(PING_PROTOCOL_CONDITION)) {
      return InterledgerFulfillPacket.builder()
        .fulfillment(PING_PROTOCOL_FULFILLMENT)
        .data(preparePacket.getData())
        .build();
    } else {
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .message("Invalid Ping Protocol Condition")
        .triggeredBy(getLinkSettings().getOperatorAddress())
        .build();
    }
  }
}
