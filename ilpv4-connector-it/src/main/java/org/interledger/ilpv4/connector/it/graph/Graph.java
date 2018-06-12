package org.interledger.ilpv4.connector.it.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of nodes and edges that connect those nodes.
 */
public class Graph {

  private final Map<String, Node> nodes = new HashMap<>();
  private final List<Edge> edges = new ArrayList<>();

  public Graph addNode(String key, Node node) {
    nodes.put(key, node);
    return this;
  }

  public Graph addEdge(Edge edge) {
    edges.add(edge);
    return this;
  }

  public Node getNode(String key) {
    return nodes.get(key);
  }

  public Graph start() {
    for (Node node : nodes.values()) {
      node.start();
    }
    for (Edge edge : edges) {
      edge.connect(this);
    }
    return this;
  }

  public void stop() {
    for (Node node : nodes.values()) {
      node.stop();
    }
  }
}
