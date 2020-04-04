package org.interledger.connector.it.topology;

import org.interledger.core.InterledgerAddress;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of nodes and edges that connect those nodes.
 */
public class Topology extends AbstractBaseTopology<Topology> {

  private final Map<String, Node> nodes = new HashMap<>();
  private final List<Edge> edges = new ArrayList<>();

  public Topology(final String topologyName) {
    this(topologyName, new PostConstructListener<Topology>() {
      @Override
      protected void doAfterTopologyStartup(Topology topology) {
        // do nothing by default.
      }
    });
  }

  public Topology(final String topologyName, final PostConstructListener postConstructListener) {
    super(topologyName, postConstructListener);
  }

  @Override
  public Topology addNode(String key, Node node) {
    nodes.put(key, node);
    return this;
  }

  @Override
  public Topology addNode(InterledgerAddress key, Node node) {
    nodes.put(key.getValue(), node);
    return this;
  }

  @Override
  public Topology addEdge(Edge edge) {
    edges.add(edge);
    return this;
  }

  @Override
  public <T> T getNode(String key, Class<T> clazz) {
    return (T) nodes.get(key);
  }

  @Override
  public <T> T getNode(InterledgerAddress key, Class<T> clazz) {
    return (T) nodes.get(key.getValue());
  }

  @Override
  public Collection<Node> getNodeValues() {
    return nodes.values();
  }

  @Override
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

  @Override
  public void stop() {
    for (Node node : nodes.values()) {
      node.stop();
    }
  }

}
