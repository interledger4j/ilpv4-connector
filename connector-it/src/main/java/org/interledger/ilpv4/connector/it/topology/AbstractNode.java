package org.interledger.ilpv4.connector.it.topology;

import java.util.Objects;

/**
 * A node in a topology which exposes an instance of {@link T} that can be used to interact with the Node.
 */
public abstract class AbstractNode<T> implements Node<T> {

  private final T contentObject;

  public AbstractNode(final T contentObject) {
    this.contentObject = Objects.requireNonNull(contentObject);
  }

  @Override
  public T getContentObject() {
    return this.contentObject;
  }

}
