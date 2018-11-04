package org.interledger.ilpv4.connector.it.graph.nodes;

import com.sappenin.ilpv4.client.IlpClient;
import org.interledger.ilpv4.connector.it.graph.ClientNode;
import org.interledger.ilpv4.connector.it.graph.Node;

/**
 * An implementation of {@link Node} that contains a {@link IlpClient} for simulating ILPv4 Sender operations.
 */
public class IlpSenderNode extends ClientNode implements Node {

  public IlpSenderNode(final IlpClient ilpClient) {
    super(ilpClient);
  }
}
