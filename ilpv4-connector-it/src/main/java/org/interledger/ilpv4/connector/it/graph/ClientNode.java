package org.interledger.ilpv4.connector.it.graph;

import com.sappenin.ilpv4.client.IlpClient;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * An implementation of {@link Node} that contains a {@link IlpClient} for simulating ILPv4 client operations.
 *
 * @author sappenin
 */
public class ClientNode implements Node {

  private final IlpClient ilpClient;

  public ClientNode(final IlpClient ilpClient) {
    this.ilpClient = Objects.requireNonNull(ilpClient);
  }

  @Override
  public void start() {
    //ilpClient.connect();
  }

  @Override
  public void stop() {
//    try {
//      ilpClient.disconnect().get();
//    } catch (InterruptedException | ExecutionException e) {
//      throw new RuntimeException(e);
//    }
  }

  public IlpClient getIlpClient() {
    return this.ilpClient;
  }
}
