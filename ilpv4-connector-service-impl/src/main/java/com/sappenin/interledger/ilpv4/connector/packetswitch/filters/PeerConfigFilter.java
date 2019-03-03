package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>A {@link PacketSwitchFilter} that implements the ILP Peer-Config sub-protocol, which allows a Connector to
 * support IL-DCP in order to provide a particular link with an Interledger address rooted in this Connector's
 * namespace.</p>
 *
 * <p>Note: This link functions like the `ildcpHostController` in the Javascript implementation.</p>
 */
public class PeerConfigFilter implements PacketSwitchFilter {

  public static final InterledgerAddress PEER_DOT_CONFIG = InterledgerAddress.of("peer.config");
  public static final InterledgerAddressPrefix PEER_DOT_ROUTE_PREFIX = InterledgerAddressPrefix.from(PEER_DOT_CONFIG);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<InterledgerAddress> nodeOperatorAddressSupplier;

  /**
   * Required-args Constructor.
   */
  public PeerConfigFilter(final Supplier<InterledgerAddress> nodeOperatorAddressSupplier) {
    this.nodeOperatorAddressSupplier = Objects.requireNonNull(nodeOperatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(sourcePreparePacket);
    Objects.requireNonNull(filterChain);

    // TODO - FIXME!
    return filterChain.doFilter(sourceAccountId, sourcePreparePacket);
  }

}
