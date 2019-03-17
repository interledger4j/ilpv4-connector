package org.interledger.ilpv4.connector.it.topology;

import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;

import java.util.Objects;

/**
 * A node in a topology which exposes an instance of {@link Link} that can be used to interact with the Node.
 */
public class LinkNode<PS extends LinkSettings, P extends Link<PS>> extends AbstractNode<P> implements Node<P> {

  public LinkNode(final P contentObject) {
    super(contentObject);
  }

  @Override
  public String getId() {
    return getContentObject().getLinkId().map(Objects::toString)
      .orElseThrow(() -> new RuntimeException("Link must have an id!"));
  }

  /**
   * Start this node. Sometimes a node is started when the topology starts-up, but not always (e.g., a delayed start).
   */
  @Override
  public void start() {
    getContentObject().connect().join();
  }

  /**
   * Stop this node.
   */
  @Override
  public void stop() {
    getContentObject().disconnect().join();
  }


}
