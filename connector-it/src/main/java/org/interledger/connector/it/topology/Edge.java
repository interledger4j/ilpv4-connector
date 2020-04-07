package org.interledger.connector.it.topology;

/**
 * A connection between two Nodes in a topology.
 *
 * @deprecated Edges are no longer needed since the Topology typically determines who is connected to whom. If new
 * connections need to be created, then tests should grab a node, and utilize the admin API.
 */
@Deprecated
public abstract class Edge {
  public abstract boolean isConnected();

  /**
   * Connect this edge to the topology.
   *
   * @param topology
   */
  public abstract void connect(AbstractBaseTopology topology);
}
