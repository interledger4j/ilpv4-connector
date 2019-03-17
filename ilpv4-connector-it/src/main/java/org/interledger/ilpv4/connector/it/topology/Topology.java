package org.interledger.ilpv4.connector.it.topology;

import com.google.common.collect.Lists;
import org.interledger.core.InterledgerAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of nodes and edges that connect those nodes.
 */
public class Topology {

  private final Map<String, Node> nodes = new HashMap<>();
  private final List<Edge> edges = new ArrayList<>();
  private final PostConstructListener postConstructListener;

  public Topology() {
    this(new PostConstructListener() {
      @Override
      protected void doAfterTopologyStartup(Topology topology) {
        // do nothing by default.
      }
    });
  }

  public Topology(final PostConstructListener postConstructListener) {
    this.postConstructListener = Objects.requireNonNull(postConstructListener);
  }

  public Topology addNode(String key, Node node) {
    nodes.put(key, node);
    return this;
  }

  public Topology addNode(InterledgerAddress key, Node node) {
    nodes.put(key.getValue(), node);
    return this;
  }

  public Topology addEdge(Edge edge) {
    edges.add(edge);
    return this;
  }

  public Node getNode(String key) {
    return nodes.get(key);
  }

  public <T> T getNode(String key, Class<T> clazz) {
    return (T) nodes.get(key);
  }

  public Node getNode(InterledgerAddress key) {
    return getNode(key.getValue());
  }

  public <T> T getNode(InterledgerAddress key, Class<T> clazz) {
    return (T) nodes.get(key);
  }

  public Collection<Node> getNodeValues() {
    return nodes.values();
  }

  public Topology start() {
    List<Node> nodeList = Lists.newArrayList(nodes.values());
    Collections.sort(nodeList, Comparator.comparing(Node::getId));

    for (Node node : nodeList) {
      node.start();
    }

    for (Edge edge : edges) {
      edge.connect(this);
    }

    // Notify the listener...
    postConstructListener.afterTopologyStartup(this);

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
   * Allows the test-harness to addAccount edges _after_ the topology has started. This is useful for things like adding
   * a plugin, which might need to know the port of a peering server, which isn't known until after the Spring container
   * has started.
   */
  public static abstract class PostConstructListener {

    public final void afterTopologyStartup(Topology topology) {
      this.doAfterTopologyStartup(topology);

      // Connect any unconnected edges...
      for (Edge edge : topology.getEdges()) {
        if (!edge.isConnected()) {
          edge.connect(topology);
        }
      }
    }

    protected abstract void doAfterTopologyStartup(final Topology topology);

  }
}
