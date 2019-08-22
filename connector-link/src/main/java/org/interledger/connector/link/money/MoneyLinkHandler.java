package org.interledger.connector.link.money;

import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerPreparePacket;

import java.math.BigInteger;

/**
 * Handles an incoming {@link InterledgerPreparePacket} for a single plugin, sent from a remote peer.
 *
 * @deprecated Consider an interface outside of {@link Link} for settlement.
 */
@Deprecated
@FunctionalInterface
public interface MoneyLinkHandler {

  /**
   * Handles an incoming {@code amount} of units that a peer has supposedly sent this node via some settlement
   * mechanism.
   *
   * @param amount A {@link BigInteger} representing the amount that the counterparty has settled.
   *
   * @return
   */
  void handleIncomingMoney(BigInteger amount);
}
