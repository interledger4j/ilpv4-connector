package org.interledger.connector.connections;

/**
 * An implementation of {@link BilateralConnection} that simulates a gRPC Bilateral Push connection with a remote node.
 * In this setup, each node will have both a client and a server, so incoming and outgoing data is sent along distinct
 * network paths (in this case, the MultiplexedBilateralSender and MultiplexedBilateralReceiver are different
 * instances).
 */
@Deprecated
public class SimulatedBppConnection {
//  extends AbstractBilateralConnection<MultiplexedBilateralReceiver, MultiplexedBilateralSender, SimulatedPlugin>
//  implements BilateralConnection<MultiplexedBilateralReceiver, MultiplexedBilateralSender, SimulatedPlugin> {
//
//  public static final String CONNECTION_TYPE_STRING = "SimulatedServerConnection";
//  public static final BilateralConnectionType CONNECTION_TYPE = BilateralConnectionType.of(CONNECTION_TYPE_STRING);
//
//  // For simulation purposes, allows a test-harness to flip this flag in order to simulate failed operations.
//  private ExpectedConnectionState expectedConnectionState = ExpectedConnectionState.UP;
//
//  /**
//   * Required-args Constructor.
//   */
//  public SimulatedBppConnection(final InterledgerAddress operatorAddress) {
//    super(operatorAddress, new SimulatedMultiplexedBilateralReceiver(), new SimulatedMultiplexedBilateralSender());
//  }
//
//  public void setExpectedConnectionState(final ExpectedConnectionState expectedConnectionState) {
//    this.expectedConnectionState = Objects.requireNonNull(expectedConnectionState);
//    if (expectedConnectionState == ExpectedConnectionState.UP) {
//      this.connect();
//    } else {
//      this.disconnect();
//    }
//  }
//
//  /**
//   * Unregister the {@link Plugin} that was handling LPIv2 tasks for the account identified by {@code account}.
//   *
//   * @param accountAddress The address of the account to unregister links for.
//   */
//  @Override
//  public void unregisterPlugin(InterledgerAddress accountAddress) {
//
//  }
//
//  /**
//   * Called by an external process to simulate an incoming data packet from the remote. In this way, a test or other
//   * process can emulate playing the role of a client making a call into this simulated connection.
//   */
//  public CompletableFuture<Optional<InterledgerResponsePacket>> simulateIncomingData(
//    final InterledgerAddress sourceAccount, final InterledgerPreparePacket outgoingPreparePacket
//  ) {
//    return this.getMultiplexedBilateralReceiver()
//      .getBilateralReceiver(sourceAccount)
//      .map(BilateralReceiver::getDataHandler)
//      .filter(Optional::isPresent)
//      .map(Optional::get)
//      .map(handler -> handler.handleIncomingPacket(outgoingPreparePacket))
//      .orElseThrow(() -> new RuntimeException("No DataHandler present!"));
//  }
//
//  enum ExpectedConnectionState {
//    UP,
//    DOWN
//  }
//
//  private static class SimulatedMultiplexedBilateralSender extends AbstractMultiplexedBilateralSender
//    implements MultiplexedBilateralSender {
//  }
//
//  // This receiver is playing the role of a gRPC Server. Thus, it will be receiving a packet
//  private static class SimulatedMultiplexedBilateralReceiver extends AbstractMultiplexedBilateralReceiver implements
//    MultiplexedBilateralReceiver {
//
//  }


}
