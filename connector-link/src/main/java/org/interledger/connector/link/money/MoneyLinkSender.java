package org.interledger.connector.link.money;

import org.interledger.connector.link.Link;

import java.math.BigInteger;

/**
 * Defines how to send money to (i.e., settle with) the other side of a bilateral connection (i.e., the other party
 * operating a single account in tandem with the operator of this sender).
 *
 * @deprecated Consider an interface outside of {@link Link} for settlement.
 */
@Deprecated
@FunctionalInterface
public interface MoneyLinkSender {

  /**
   * Settle an outstanding ILP balance with a counterparty by transferring {@code amount} units of value from this ILP
   * node to the counterparty of the account used by this plugin (this method correlates to <tt>sendMoney</tt> in the
   * Javascript Connector).
   *
   * @param amount The amount of "money" to transfer via some settlement mechanism.
   */
  void sendMoney(BigInteger amount);

}
