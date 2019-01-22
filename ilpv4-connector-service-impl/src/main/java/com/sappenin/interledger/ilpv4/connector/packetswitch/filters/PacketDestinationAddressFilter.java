package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


/**
 * An implementation of {@link SendDataFilter} for intercepting packets that should not enter into the Connector's
 * Switching Fabric. These include
 */
public class PacketDestinationAddressFilter implements SendDataFilter {

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;

  public PacketDestinationAddressFilter(final Supplier<ConnectorSettings> connectorSettingsSupplier) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket incomingPreparePacket,
    final SendDataFilterChain filterChain
  ) {

    final InterledgerAddress destinationAddress = incomingPreparePacket.getDestination();

    // See Design README.md: `example` is not allowed.
    if (destinationAddress.startsWith(InterledgerAddressPrefix.EXAMPLE.getValue())) {
      return CompletableFuture.completedFuture(Optional.of(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F02_UNREACHABLE)
        .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
        .message("`example.` addresses are not accepted by this Connector.")
        .build()));
    }
    // See Design README.md: `example` is not allowed.
    else if (destinationAddress.startsWith(InterledgerAddressPrefix.EXAMPLE.getValue())) {
      return CompletableFuture.completedFuture(Optional.of(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F02_UNREACHABLE)
        .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
        .message("`example.` addresses are not accepted by this Connector.")
        .build()));
    }


    // For now, this is just a pass-through.
    // TODO: Implement Balance tracking logic.

    // Call the next filter in the chain...
    return filterChain.doFilter(sourceAccountId, incomingPreparePacket);
  }
}
