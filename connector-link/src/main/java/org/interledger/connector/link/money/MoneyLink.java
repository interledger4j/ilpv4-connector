package org.interledger.connector.link.money;

import org.interledger.connector.link.Link;

import java.math.BigInteger;
import java.util.Optional;

/**
 * <p>An abstraction for communicating settlement information with a remote Interledger peer using a single
 * account.</p>
 *
 * <p>The overall flow of funds in ILPv4 is as follows:
 *
 * <pre>
 * Sender --sendMoney-> Connector 1 --sendMoney-> Connector 2 --sendMoney-> Receiver
 *    |                        |                        |
 *    `----sendMoney->         `----sendMoney->         `----sendMoney->
 * </pre>
 *
 * Sender/Connector's call <tt>sendMoney</tt>, wait for a fulfillment, and then call <tt>sendMoney</tt> (possibly
 * infrequently or even only eventually for bulk settlement) if the fulfillment is valid.</p>
 *
 * @deprecated Consider an interface outside of {@link Link} for settlement.
 */
@Deprecated
public interface MoneyLink<LS extends MoneyLinkSettings> extends MoneyLinkSender {

  /**
   * <p>Set the callback which is used to handle incoming prepared money packets. The handler should expect one
   * parameter (an ILP Prepare Packet) and return a CompletableFuture for the resulting response. If an error occurs,
   * the callback MAY throw an exception. In general, the callback should behave as {@link
   * MoneyLink#sendMoney(BigInteger)} does.</p>
   *
   * <p>If a money handler is already set, this method throws a <tt>MoneyHandlerAlreadyRegisteredException</tt>. In
   * order to change the money handler, the old handler must first be removed via {@link #unregisterMoneyHandler()}.
   * This is to ensure that handlers are not overwritten by accident.</p>
   *
   * <p>If an incoming packet is received by the link, but no handler is registered, the link SHOULD respond with
   * an error.</p>
   *
   * @param moneyHandler An instance of {@link MoneyLinkHandler}.
   */
  void registerMoneyHandler(MoneyLinkHandler moneyHandler); //throws MoneyHandlerAlreadyRegisteredException;

  /**
   * Accessor for the currently registered (though optionally-present) {@link MoneyLinkHandler}.
   *
   * @return An optionally-present {@link MoneyLinkHandler}.
   */
  Optional<MoneyLinkHandler> getMoneyHandler();

  /**
   * Accessor for the currently registered (though optionally-present) {@link MoneyLinkHandler}.
   *
   * @return The currently registered {@link MoneyLinkHandler}.
   *
   * @throws RuntimeException if no handler is registered (A MoneyLink is not in a valid state until it has handlers
   *                          registered)
   */
  default MoneyLinkHandler safeGetMoneyHandler() {
    return this.getMoneyHandler()
      .orElseThrow(() -> new RuntimeException("You MUST register a MoneyHandler before using this link!"));
  }

  /**
   * Removes the currently used {@link MoneyLinkHandler}. This has the same effect as if {@link
   * #registerMoneyHandler(MoneyLinkHandler)} had never been called. If no money handler is currently set, this method
   * does nothing.
   */
  void unregisterMoneyHandler();
}
