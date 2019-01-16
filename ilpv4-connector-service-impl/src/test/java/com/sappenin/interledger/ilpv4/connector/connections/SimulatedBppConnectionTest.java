package com.sappenin.interledger.ilpv4.connector.connections;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketHandler;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link SimulatedBppConnection} that is operating two bilateral sender/receivers, one representing a
 * USD account and another representing an EUR account.
 */
public class SimulatedBppConnectionTest {

//  public static final byte[] USD_PREIMAGE = "Why don't you make like a tree a".getBytes();
//  public static final InterledgerFulfillment USD_FULFILLMENT = InterledgerFulfillment.of(USD_PREIMAGE);
//
//  public static final byte[] EUR_PREIMAGE = "Roads? Where we're going we dont".getBytes();
//  public static final InterledgerFulfillment EUR_FULFILLMENT = InterledgerFulfillment.of(EUR_PREIMAGE);
//
//  private static final InterledgerPreparePacket USD_PREPARE_PACKET = InterledgerPreparePacket.builder()
//    .executionCondition(SimulatedPlugin.FULFILLMENT.getCondition())
//    .expiresAt(Instant.now().plus(30, ChronoUnit.SECONDS))
//    .destination(InterledgerAddress.of("test.foo"))
//    .amount(BigInteger.TEN)
//    .build();
//  private static final InterledgerPreparePacket EUR_PREPARE_PACKET = InterledgerPreparePacket.builder()
//    .executionCondition(SimulatedPlugin.FULFILLMENT.getCondition())
//    .expiresAt(Instant.now().plus(30, ChronoUnit.SECONDS))
//    .destination(InterledgerAddress.of("test.foo"))
//    .amount(BigInteger.TEN)
//    .build();
//
//
//  // Simulates an account with a peer called `mypeer`
//  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test1.connector.mypeer");
//
//  // Simulates a USD account with the peer
//  private static final InterledgerAddress USD_ACCOUNT_ADDRESS = OPERATOR_ADDRESS.with("usd");
//
//  // Simulates a EUR account with the peer
//  private static final InterledgerAddress EUR_ACCOUNT_ADDRESS = OPERATOR_ADDRESS.with("eur");
//
//  private SimulatedBppConnection connection;
//
//  @Before
//  public void setup() {
//    connection = new SimulatedBppConnection(OPERATOR_ADDRESS);
//    // By default, simulate this connection is UP!
//    connection.setExpectedConnectionState(SimulatedBppConnection.ExpectedConnectionState.UP);
//
//    // In the Connection, register the usdAccountPlugin with the USD_ACCOUNT_ADDRESS.
//    {
//      final PluginSettings pluginSettings = PluginSettings.builder()
//        .pluginType(SimulatedPlugin.PLUGIN_TYPE)
//        .localNodeAddress(OPERATOR_ADDRESS)
//        .accountAddress(USD_ACCOUNT_ADDRESS)
//        .build();
//      final SimulatedPlugin usdAccountPlugin = new SimulatedPlugin(pluginSettings);
//      connection.registerPlugin(USD_ACCOUNT_ADDRESS, usdAccountPlugin);
//    }
//    {
//      final PluginSettings pluginSettings = PluginSettings.builder()
//        .pluginType(SimulatedPlugin.PLUGIN_TYPE)
//        .localNodeAddress(OPERATOR_ADDRESS)
//        .accountAddress(USD_ACCOUNT_ADDRESS)
//        .build();
//      final SimulatedPlugin usdAccountPlugin = new SimulatedPlugin(pluginSettings);
//      connection.registerPlugin(USD_ACCOUNT_ADDRESS, usdAccountPlugin);
//    }
//  }
//
//  /**
//   * This test simulates incoming data packets over two accounts communicating over the same {@link
//   * BilateralConnection}.
//   *
//   * The first receiver operates on behalf of `test1.connector.mypeer.usd`, and the second receiver operates on behalf
//   * of `test1.connector.mypeer.eur`.
//   */
//  @Test
//  public void testIncomingPacketWhileConnectionIsUp() {
//    {
//      final Optional<InterledgerResponsePacket> responsePacket =
//        connection.simulateIncomingData(USD_ACCOUNT_ADDRESS, USD_PREPARE_PACKET).join();
//
//      new InterledgerResponsePacketHandler() {
//        @Override
//        protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
//          assertThat(interledgerFulfillPacket.getFulfillment(), is(USD_FULFILLMENT));
//        }
//
//        @Override
//        protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
//          throw new RuntimeException("Should not Reject!");
//        }
//
//        @Override
//        protected void handleExpiredPacket() {
//          throw new RuntimeException("Should not Expire!");
//        }
//      }.handle(responsePacket);
//    }
//    {
//      // Simulate the EUR packet incoming...
//      final Optional<InterledgerResponsePacket> responsePacket =
//        connection.simulateIncomingData(EUR_ACCOUNT_ADDRESS, EUR_PREPARE_PACKET).join();
//      new InterledgerResponsePacketHandler() {
//        @Override
//        protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
//          assertThat(interledgerFulfillPacket.getFulfillment(), is(EUR_FULFILLMENT));
//        }
//
//        @Override
//        protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
//          throw new RuntimeException("Should not Reject!");
//        }
//
//        @Override
//        protected void handleExpiredPacket() {
//          throw new RuntimeException("Should not Expire!");
//        }
//      }.handle(responsePacket);
//    }
//
//  }


}