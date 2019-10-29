package org.interledger.connector.it.topology;

import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.link.StatefulLink;

/**
 * A node in a topology which exposes an instance of {@link Link} that can be used to interact with the Node.
 */
public class LinkNode<PS extends LinkSettings, P extends Link<PS>> extends AbstractNode<P> implements Node<P> {

  public LinkNode(final P contentObject) {
    super(contentObject);
  }

  @Override
  public String getId() {
    return getContentObject().getLinkId().value();
  }

  /**
   * Start this node. Sometimes a node is started when the topology starts-up, but not always (e.g., a delayed start).
   */
  @Override
  public void start() {
    if (getContentObject() instanceof StatefulLink) {
      ((StatefulLink) getContentObject()).connect().join();
    }
  }

  /**
   * Stop this node.
   */
  @Override
  public void stop() {
    if (getContentObject() instanceof StatefulLink) {
      ((StatefulLink) getContentObject()).disconnect().join();
    }
  }


}
