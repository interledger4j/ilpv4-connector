package org.interledger.ilpv4;

import org.interledger.core.InterledgerAddress;

import java.util.Optional;

/**
 * <p>A Node is a general term for any participant in the Interledger. A node may be a sender, a receiver, or both.
 * It might also be a connector. The node may represent a person or business, or perhaps a single device or software
 * program. In the case where a node represents a device or software program, the node is usually connected to another
 * node that represents the device's owner or the person running the software.</p>
 *
 * The Interledger network can be envisioned as a topology where the points are individual nodes and the edges are
 * accounts between two parties. Parties with only one account can send or receive through the party on the other side
 * of that account. Parties with two or more accounts are connectors, who can facilitate payments to or from anyone
 * they're connected to.</p>
 *
 * <p>A node is neither a direction extension for a sender/receiver or other primitives, nor inherits from them,
 * in order to allow implementations to always be a node, while only sometimes being a sender, receiver, or other
 * interface.</p>
 */
public interface ILPv4Node {

  /**
   * The {@link InterledgerAddress} of this node, if present.
   *
   * @return
   */
  Optional<InterledgerAddress> getNodeIlpAddress();
}
