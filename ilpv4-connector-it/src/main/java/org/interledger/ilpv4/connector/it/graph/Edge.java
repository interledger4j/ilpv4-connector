package org.interledger.ilpv4.connector.it.graph;

/**
 * A connection between two Nodes in a graph.
 */
public abstract class Edge {
  public abstract boolean isConnected();

  public abstract void connect(Graph graph);
}
