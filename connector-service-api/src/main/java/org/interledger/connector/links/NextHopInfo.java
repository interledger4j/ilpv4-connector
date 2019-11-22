package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;

import org.immutables.value.Value;

/**
 * A container that holds the next-hop packet (with a final destination) as well as the address of the next-hop account
 * to send the packet to.
 */
@Value.Immutable
public interface NextHopInfo {

  static ImmutableNextHopInfo.Builder builder() {
    return ImmutableNextHopInfo.builder();
  }

  /**
   * The {@link InterledgerAddress} of the next-hop account to send a prepare packet to.
   */
  AccountId nextHopAccountId();

  /**
   * The packet to send to the next hop.
   */
  InterledgerPreparePacket nextHopPacket();

}

