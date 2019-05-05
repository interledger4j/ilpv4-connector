package com.sappenin.interledger.ilpv4.connector.links.loopback;

import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.AbstractLink;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkHandler;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * <p>A {@link Link} that always responds with a Fulfillment that contains the data supplied by the Prepare packet.</p>
 */
public class LoopbackLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "LOOPBACK";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerFulfillment LOOPBACK_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  private static final String SIMULATE_TIMEOUT = "simulateTimeout";

  private final PacketRejector packetRejector;

  /**
   * Required-args constructor.
   */
  public LoopbackLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final LinkSettings linkSettings,
    final LinkEventEmitter linkEventEmitter,
    final PacketRejector packetRejector
  ) {
    super(operatorAddressSupplier, linkSettings, linkEventEmitter);
    this.packetRejector = Objects.requireNonNull(packetRejector);
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

    return Optional.ofNullable(this.getLinkSettings().getCustomSettings().get(SIMULATE_TIMEOUT))
      .map((value) -> {
        if (value.equals("T02")) {
          return packetRejector.reject(AccountId.of(getLinkId().value()), preparePacket,
            InterledgerErrorCode.T02_PEER_BUSY, "Loopback set to manually reject via simulate_timeout=T02");
        } else if (value.equals("T03")) {
          // Sleep for 1 minute, which in the typical case will exceed the Circuit-breaker's threshold.
          try {
            Thread.sleep(60000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          return packetRejector.reject(AccountId.of(getLinkId().value()), preparePacket,
            InterledgerErrorCode.T03_CONNECTOR_BUSY, "Loopback set to exceed timeout via simulate_timeout=T03");
        }
        if (value.equals("T99")) {
          throw new RuntimeException("T99 APPLICATION ERROR");
        } else {
          return InterledgerFulfillPacket.builder()
            .fulfillment(LOOPBACK_FULFILLMENT)
            .data(preparePacket.getData())
            .build();
        }
      })
      .orElseGet(InterledgerFulfillPacket.builder()
        .fulfillment(LOOPBACK_FULFILLMENT)
        .data(preparePacket.getData())::build);

    // if (preparePacket.getAmount().equals(BigInteger.ZERO)) {
    //    } else {
    //      return InterledgerRejectPacket.builder()
    //        .code(InterledgerErrorCode.F00_BAD_REQUEST)
    //        .message("Loopback Packets MUST have an amount of 0")
    //        .triggeredBy(getOperatorAddressSupplier().get().get()) // We expect the address to have been populated.
    //        .build();
    //    }
  }
}
