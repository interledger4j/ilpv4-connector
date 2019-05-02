package org.interledger.connector.link;

import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * <p>An abstraction for communicating with a remote Interledger peer using a single account.</p>
 *
 * <p>The overall flow of funds in ILPv4 is as follows:
 *
 * <pre>
 * Sender --sendPacket-> Connector 1 --sendPacket-> Connector 2 --sendPacket-> Receiver
 *    |                        |                        |
 *    `----sendMoney->         `----sendMoney->         `----sendMoney->
 * </pre>
 *
 * Sender/Connector's call <tt>sendPacket</tt>, wait for a fulfillment, and then call <tt>sendMoney</tt> (possibly
 * infrequently or even only eventually for bulk settlement) if the fulfillment is valid.</p>
 */
public interface Link<LS extends LinkSettings> extends LinkSender, Connectable {

  /**
   * A unique identifier for this {@link Link}. This value is generally set only once, (e.g., in a Connector to
   * correlate a Link to an account identifier) so that the same link can be referenced across requests.
   *
   * @return A {@link LinkId}.
   */
  Optional<LinkId> getLinkId();

  /**
   * A supplier fori the ILP address of this node operator. This value may be empty, for example, in cases where the Link
   * obtains its address from a parent node using IL-DCP.
   *
   * @return A {@link Supplier} of an optioanally-present {@link InterledgerAddress}.
   */
  Supplier<Optional<InterledgerAddress>> getOperatorAddressSupplier();

  /**
   * The settings for this Link.
   */
  LS getLinkSettings();

  /**
   * <p>Set the callback which is used to handle incoming prepared data packets. The handler should expect one
   * parameter (an ILP Prepare Packet) and return a CompletableFuture for the resulting response. If an error occurs,
   * the callback MAY throw an exception. In general, the callback should behave as {@link
   * Link#sendPacket(InterledgerPreparePacket)} does.</p>
   *
   * <p>If a data handler is already set, this method throws a {@link LinkHandlerAlreadyRegisteredException}. In order
   * to change the data handler, the old handler must first be removed via {@link #unregisterLinkHandler()}. This is to
   * ensure that handlers are not overwritten by accident.</p>
   *
   * <p>If an incoming packet is received by the link, but no handler is registered, the link SHOULD respond with
   * an error.</p>
   *
   * @param dataHandler An instance of {@link LinkHandler}.
   */
  void registerLinkHandler(LinkHandler dataHandler) throws LinkHandlerAlreadyRegisteredException;

  /**
   * Accessor for the currently registered (though optionally-present) {@link LinkHandler}.
   *
   * @return An optionally-present {@link LinkHandler}.
   */
  Optional<LinkHandler> getLinkHandler();

  /**
   * Accessor for the currently registered (though optionally-present) {@link LinkHandler}.
   *
   * @return The currently registered {@link LinkHandler}.
   *
   * @throws RuntimeException if no handler is registered (A Link is not in a valid state until it has handlers
   *                          registered)
   */
  default LinkHandler safeGetLinkHandler() {
    return this.getLinkHandler()
      .orElseThrow(() -> new RuntimeException("You MUST register a LinkHandler before using this link!"));
  }

  /**
   * Removes the currently used {@link LinkHandler}. This has the same effect as if {@link
   * #registerLinkHandler(LinkHandler)} had never been called. If no link handler is currently set, this method does
   * nothing.
   */
  void unregisterLinkHandler();

  /**
   * Add a  link event handler to this link.
   *
   * Care should be taken when adding multiple handlers to ensure that they perform distinct operations, otherwise
   * duplicate functionality might be unintentionally introduced.
   *
   * @param eventListener A {@link LinkEventListener} that can handle various types of events emitted by this ledger
   *                      link.
   *
   * @return A {@link UUID} representing the unique identifier of the handler, as seen by this ledger link.
   */
  void addLinkEventListener(LinkEventListener eventListener);

  /**
   * Removes an event handler from the collection of handlers registered with this ledger link.
   *
   * @param eventListener A {@link LinkEventListener} representing the listener to remove.
   */
  void removeLinkEventListener(LinkEventListener eventListener);

}
