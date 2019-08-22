package org.interledger.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

/**
 * <p>Provides a view into the invocation chain of a filtered Interledger Prepare/Fulfill/Reject request/response
 * flow.</p>
 *
 * <p>Filters use the this contract to invoke the next filter in the chain, or if the calling filter is the last filter
 * in the chain, to invoke {@link Link#sendPacket(InterledgerPreparePacket)} on the appropriate outbound link, which
 * exists at the end of the chain.</p>
 *
 * <p>An example filter chain for a typical Interledger Connector looks like this:</p>
 *
 * <pre>
 *   ┌────────────────────────────────────┐
 *   │   AllowedDestinationPacketFilter   │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │       MaxPacketAmountFilter        │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │         ExpiryPacketFilter         │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │       BalanceIlpPacketFilter       │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │  ValidateFulfillmentPacketFilter   │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │      PeerProtocolPacketFilter      │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽──┐
 *   │                                   │
 *   │           PacketSwitch            │
 *   │                                   │
 *   └───────────────────────────────────┘
 * </pre>
 *
 * @see PacketSwitchFilter
 **/
public interface PacketSwitchFilterChain {

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param sourceAccountSettings The source {@link AccountSettings} that this incoming Prepare packet is being
   *                              processed for.
   * @param preparePacket         The incoming {@link InterledgerPreparePacket} that was accepted from an incoming
   *                              account/link.
   *
   * @return An optionally-present {@link InterledgerResponsePacket}.
   */
  InterledgerResponsePacket doFilter(AccountSettings sourceAccountSettings, InterledgerPreparePacket preparePacket);
}
