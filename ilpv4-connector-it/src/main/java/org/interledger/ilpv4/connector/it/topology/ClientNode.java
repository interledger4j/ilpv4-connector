package org.interledger.ilpv4.connector.it.topology;

import org.interledger.plugin.lpiv2.Plugin;

/**
 * A node in a topology which exposes one or more instances of {@link Plugin} that can be used to interact with the Node.
 *
 * @deprecated It is no longer necessary to distinguish a ClientNode. If a node is _not_ a ServerNode, it should just
 * extend {@link Node} directly.
 */
@Deprecated
public interface ClientNode<T> extends Node<T> {


}
