package org.interledger.connector.it.topology.nodes;

import org.interledger.connector.it.topology.LinkNode;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

/**
 * An extension of {@link LinkNode} that is more narrowly typed.
 */
public class BlastLinkNode extends LinkNode<IlpOverHttpLinkSettings, IlpOverHttpLink> {

  public BlastLinkNode(final IlpOverHttpLink blastLink) {
    super(blastLink);
  }

}
