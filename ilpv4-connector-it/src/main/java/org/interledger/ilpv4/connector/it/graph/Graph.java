package org.interledger.ilpv4.connector.it.graph;

import java.util.*;

/**
 * A collection of nodes and edges that connect those nodes.
 */
public class Graph {

  private final Map<String, Node> nodes = new HashMap<>();
  private final List<Edge> edges = new ArrayList<>();
  private final PostConstructListener postConstructListener;

  public Graph() {
    this(new PostConstructListener() {
      @Override
      protected void doAfterGraphStartup(Graph graph) {
        // do nothing by default.
      }
    });
  }

  public Graph(final PostConstructListener postConstructListener) {
    this.postConstructListener = Objects.requireNonNull(postConstructListener);
  }

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

  public Collection<Node> getNodeValues() {
    return nodes.values();
  }

  public Graph start() {
    for (Node node : nodes.values()) {
      node.start();
    }

    for (Edge edge : edges) {
      edge.connect(this);
    }

    // Noitify the listener...
    postConstructListener.afterGraphStartup(this);

    return this;
  }

  public List<Edge> getEdges() {
    return this.edges;
  }

  public void stop() {
    for (Node node : nodes.values()) {
      node.stop();
    }
  }

  /**
   * Allows the test-harness to add edges _after_ the graph has started. This is useful for things like adding a plugin,
   * which might need to know the port of a peering server, which isn't known until after the Spring container has
   * started.
   */
  public static abstract class PostConstructListener {

    public final void afterGraphStartup(Graph graph) {
      this.doAfterGraphStartup(graph);

      // Connect any unconnected edges...
      for (Edge edge : graph.getEdges()) {
        if (!edge.isConnected()) {
          edge.connect(graph);
        }
      }
    }

    protected abstract void doAfterGraphStartup(final Graph graph);

  }
}
