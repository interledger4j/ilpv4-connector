package com.sappenin.interledger.ilpv4.connector.links.ping;

import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkHandler;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * <p>A {@link Link} that responds to Ping protocol packets that conform to the unidirectional mode.</p>
 * <p>Ping functionality is implemented as a Link so that all packet processing related to balance tracking can be
 * properly performed.
 * </p>
 */
public class UniPingLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "UNIDIRECTIONAL_PING";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT =
    InterledgerFulfillment.of(Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc="));
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = InterledgerCondition.of(
    Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  /**
   * Required-args constructor.
   */
  public UniPingLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final LinkSettings linkSettings,
    final LinkEventEmitter linkEventEmitter
  ) {
    super(operatorAddressSupplier, linkSettings, linkEventEmitter);
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
      "Ping links never have incoming data from a remote Connector, and thus should not have a registered DataHandler."
    );
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

      // Reject.
      final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
        .triggeredBy(getOperatorAddressSupplier().get().orElse(UNSET_OPERATOR_ADDRESS))
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .message("Invalid Ping Protocol Condition")
        .build();

      logger.warn(
        "Rejecting Unidirectional Ping packet: PreparePacket: {} RejectPacket: {}", preparePacket, rejectPacket
      );

      return rejectPacket;
    }
  }
}
