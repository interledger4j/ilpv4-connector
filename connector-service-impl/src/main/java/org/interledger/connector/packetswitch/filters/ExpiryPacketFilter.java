package org.interledger.connector.packetswitch.filters;

import static org.interledger.core.InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.links.filters.LinkFilterChain;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LinkId;
import org.interledger.link.PacketRejector;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;


/**
 * An implementation of {@link PacketSwitchFilter} for pre-flight-checking packet expiry. This filter is just a simple
 * pre-check to make sure the timeout of the overall packet is not 0 or negative. Because this filter runs before the
 * PacketSwitch has adjust the actual expiry of the packet, this filter is unable to verify _actual_ packet expiry.
 * Actual expiry is enforced in the {@link LinkFilterChain}.
 */
public class ExpiryPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public ExpiryPacketFilter(final PacketRejector packetRejector) {
    super(packetRejector);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountSettings sourceAccountSettings,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    Objects.requireNonNull(sourceAccountSettings);
    Objects.requireNonNull(sourcePreparePacket);
    Objects.requireNonNull(filterChain);

    final Duration timeoutDuration = Duration.between(Instant.now(), sourcePreparePacket.getExpiresAt());
    if (timeoutDuration.isNegative() || timeoutDuration.isZero()) {
      return packetRejector.reject(
        LinkId.of(sourceAccountSettings.accountId().value()),
        sourcePreparePacket,
        R02_INSUFFICIENT_TIMEOUT,
        "The connector could not forward the payment, because the timeout was too low to subtract its safety margin"
      );
    }

    return filterChain.doFilter(sourceAccountSettings, sourcePreparePacket);
  }
}
