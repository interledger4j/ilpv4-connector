package org.interledger.connector.it.topology.nodes;

import org.interledger.connector.it.topology.LinkNode;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

/**
 * An extension of {@link LinkNode} that is more narrowly typed.
 */
public class IlpOverHttpLinkNode extends LinkNode<IlpOverHttpLinkSettings, IlpOverHttpLink> {

  public IlpOverHttpLinkNode(final IlpOverHttpLink ilpOverHttpLink) {
    super(ilpOverHttpLink);
  }

}
