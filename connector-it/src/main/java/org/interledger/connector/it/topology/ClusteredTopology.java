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
import java.util.stream.Collectors;

/**
 * A collection of nodes and edges that connect those nodes.
 */
public class ClusteredTopology extends AbstractBaseTopology<ClusteredTopology> {

  private final Map<String, List<Node>> nodes = new HashMap<>();

  public ClusteredTopology(final String topologyName) {
    this(topologyName, new PostConstructListener<ClusteredTopology>() {
      @Override
      protected void doAfterTopologyStartup(ClusteredTopology topology) {
        // do nothing by default.
      }
    });
  }

  public ClusteredTopology(final String topologyName, final PostConstructListener postConstructListener) {
    super(topologyName, postConstructListener);
  }

  @Override
  public ClusteredTopology addNode(String key, Node node) {
    List<Node> nodes = this.nodes.computeIfAbsent(key, (k) -> new ArrayList<>());
    nodes.add(node);
    return this;
  }

  @Override
  public ClusteredTopology addNode(InterledgerAddress key, Node node) {
    return addNode(key.getValue(), node);
  }

  @Override
  public <T> T getNode(String key, Class<T> clazz) {
    return (T) nodes.get(key).stream().findFirst().orElse(null);
  }

  @Override
  public <T> T getNode(InterledgerAddress key, Class<T> clazz) {
    return (T) nodes.get(key.getValue());
  }

  public <T> List<T> getNodes(String key, Class<T> clazz) {
    return (List<T>) nodes.get(key);
  }

  public <T> List<T> getNodes(InterledgerAddress key, Class<T> clazz) {
    return (List<T>) nodes.get(key.getValue());
  }

  @Override
  public Collection<Node> getNodeValues() {
    return nodes.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  @Override
  public ClusteredTopology start() {
    List<Node> nodeList = Lists.newArrayList(getNodeValues());
    Collections.sort(nodeList, Comparator.comparing(Node::getId));

    for (Node node : nodeList) {
      node.start();
    }

    // Notify the listener...
    postConstructListener.afterTopologyStartup(this);

    return this;
  }

  @Override
  public void stop() {
    for (Node node : getNodeValues()) {
      node.stop();
    }
  }

}
