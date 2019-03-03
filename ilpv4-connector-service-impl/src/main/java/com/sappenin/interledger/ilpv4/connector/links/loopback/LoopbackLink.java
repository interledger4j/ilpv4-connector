package com.sappenin.interledger.ilpv4.connector.links.loopback;

import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkHandler;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A {@link Link} that always responds with a Fulfillment, as long as the amount in the incoming prepare packet is
 * {@link java.math.BigInteger#ZERO}.</p>
 */
public class LoopbackLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "LOOP_BACK";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerFulfillment LOOPBACK_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);

  /**
   * Required-args constructor.
   */
  public LoopbackLink(final LinkSettings linkSettings) {
    super(linkSettings);
  }


  @Override
  public CompletableFuture<Void> doConnect() {
    // No-op. Internally-routed links are always connected, so there is no connect/disconnect logic required.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> doDisconnect() {
    // No-op. Internally-routed links are always connected, so there is no connect/disconnect logic required.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void registerLinkHandler(LinkHandler ilpDataHandler) throws LinkHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "Loopback links never have incoming data, and thus should not have a registered DataHandler."
    );
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    if (preparePacket.getAmount().equals(BigInteger.ZERO)) {
      return InterledgerFulfillPacket.builder()
        .fulfillment(LOOPBACK_FULFILLMENT)
        .build();
    } else {
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .message("Loopback Packets MUST have an amount of 0")
        .triggeredBy(getLinkSettings().getOperatorAddress())
        .build();
    }
  }
}
