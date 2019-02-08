package org.interledger.ilpv4.connector.it.topology.nodes;

import org.interledger.ilpv4.connector.it.topology.PluginNode;
import org.interledger.lpiv2.blast.BlastPlugin;
import org.interledger.lpiv2.blast.BlastPluginSettings;

/**
 * An extension of {@link PluginNode} that is more narrowly typed.
 */
public class BlastPluginNode extends PluginNode<BlastPluginSettings, BlastPlugin> {

  public BlastPluginNode(final BlastPlugin blastPlugin) {
    super(blastPlugin);
  }

}
