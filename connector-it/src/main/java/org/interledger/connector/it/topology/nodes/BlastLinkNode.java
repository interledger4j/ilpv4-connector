package org.interledger.connector.it.topology.nodes;

import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.it.topology.LinkNode;

/**
 * An extension of {@link LinkNode} that is more narrowly typed.
 */
public class BlastLinkNode extends LinkNode<BlastLinkSettings, BlastLink> {

  public BlastLinkNode(final BlastLink blastLink) {
    super(blastLink);
  }

}
