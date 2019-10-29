package org.interledger.connector.ping;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;

import com.google.common.primitives.UnsignedLong;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A default implementation of {@link PingInitiator}.
 */
public class DefaultPingInitiator implements PingInitiator {

  private final Link link;
  private final Supplier<Instant> expiresAtSupplier;

  public DefaultPingInitiator(
      final Link link,
      final Supplier<Instant> expiresAtSupplier
  ) {
    this.link = Objects.requireNonNull(link);
    this.expiresAtSupplier = Objects.requireNonNull(expiresAtSupplier);
  }

  @Override
  public InterledgerResponsePacket ping(
      final InterledgerAddress destinationAddress,
      final UnsignedLong pingAmount
  ) {
    Objects.requireNonNull(destinationAddress);
    Objects.requireNonNull(pingAmount);

    return this.link.sendPacket(this.constructPingPacket(destinationAddress, pingAmount, expiresAtSupplier.get()));
  }
}
