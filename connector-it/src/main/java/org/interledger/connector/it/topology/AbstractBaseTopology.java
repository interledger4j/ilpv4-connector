package org.interledger.connector.it.topology;

import org.interledger.core.InterledgerAddress;

import java.util.Collection;
import java.util.Objects;

/**
 * A collection of nodes and edges that connect those nodes.
 */
public abstract class AbstractBaseTopology<T extends AbstractBaseTopology<T>> {

  protected final String topologyName;
  protected final Topology.PostConstructListener postConstructListener;

  public AbstractBaseTopology(final String topologyName, final Topology.PostConstructListener postConstructListener) {
    this.topologyName = Objects.requireNonNull(topologyName);
    this.postConstructListener = Objects.requireNonNull(postConstructListener);
  }

  public abstract AbstractBaseTopology addNode(String key, Node node);

  public abstract AbstractBaseTopology addNode(InterledgerAddress key, Node node);

  public Node getNode(String key) {
    return getNode(key, Node.class);
  }

  public abstract <N> N getNode(String key, Class<N> clazz);

  public Node getNode(InterledgerAddress key) {
    return getNode(key, Node.class);
  }

  public abstract <N> N getNode(InterledgerAddress key, Class<N> clazz);

  public abstract Collection<Node> getNodeValues();

  public abstract T start();

  public abstract void stop();

  @Override
  public String toString() {
    return this.topologyName;
  }

  /**
   * Allows the test-harness to addAccount edges _after_ the topology has started. This is useful for things like adding
   * a plugin, which might need to know the port of a peering server, which isn't known until after the Spring container
   * has started.
   */
  public static abstract class PostConstructListener<T extends AbstractBaseTopology<T>> {

    public final void afterTopologyStartup(T topology) {
      this.doAfterTopologyStartup(topology);

      // Connect any unconnected edges...
//      for (Edge edge : topology.getEdges()) {
//        if (!edge.isConnected()) {
//          edge.connect(topology);
//        }
//      }
    }

    protected abstract void doAfterTopologyStartup(final T topology);

  }

}
