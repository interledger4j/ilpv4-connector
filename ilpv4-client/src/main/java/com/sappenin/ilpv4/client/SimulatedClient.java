package com.sappenin.ilpv4.client;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.core.*;
import org.interledger.plugin.lpiv2.AbstractPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of {@link IlpClient} that simulates a peering relationship with a parent node where this client is
 * the child.
 */
public class SimulatedClient extends AbstractPlugin<PluginSettings> implements IlpClient<PluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "SimulatedClient";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);
  public static final byte[] ILP_DATA = "MARTY!".getBytes();
  public static final byte[] PREIMAGE = "Roads? Where we're going we dont".getBytes();
  public static final InterledgerFulfillment FULFILLMENT = InterledgerFulfillment.of(PREIMAGE);

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedClient.class);

  // For simulation purposes, allows a test-harness to flip this flag in order to simulate failed operations.
  private boolean completeSuccessfully = true;

  /**
   * Required-args Constructor.
   */
  public SimulatedClient(final PluginSettings pluginSettings) {
    super(pluginSettings);

    this.registerDataHandler((sourceAccountAddress, preparePacket) -> {
      if (completeSuccessfully) {
        return getFulfillPacket();
      } else {
        throw new InterledgerProtocolException(getSendDataRejectPacket());
      }
    });

    this.registerMoneyHandler((amount -> {
      if (completeSuccessfully) {
        // No-op
        return CompletableFuture.completedFuture(null);
      } else {
        throw new InterledgerProtocolException(getSendMoneyRejectPacket());
      }
    }));

  }

  /**
   * This Mock plugin completes successfully or throws an error, depending on the setting of {@link
   * #completeSuccessfully}.
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> doSendData(InterledgerPreparePacket preparePacket)
    throws InterledgerProtocolException {
    if (completeSuccessfully) {
      return CompletableFuture.supplyAsync(() -> {
          final InterledgerFulfillPacket packet = InterledgerFulfillPacket.builder()
            .data(ILP_DATA)
            .fulfillment(FULFILLMENT)
            .build();
          return packet;
        }
      ).toCompletableFuture();
    } else {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .data(ILP_DATA)
          .triggeredBy(getPluginSettings().getPeerAccountAddress())
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .message("SendData failed!")
          .build()
      );
    }
  }

  @Override
  protected CompletableFuture<Void> doSendMoney(BigInteger amount) {
    if (completeSuccessfully) {
      return CompletableFuture.completedFuture(null);
    } else {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .data(ILP_DATA)
          .triggeredBy(getPluginSettings().getPeerAccountAddress())
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .message("SendMoney failed!")
          .build()
      );
    }
  }

  @Override
  public CompletableFuture<Void> doConnect() {
    // No-op
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> doDisconnect() {
    // No-op
    return CompletableFuture.completedFuture(null);
  }

  public void simulatedCompleteSuccessfully(final boolean completeSuccessfully) {
    this.completeSuccessfully = completeSuccessfully;
  }

  /**
   * Helper method to return the fulfill packet that is used by this simulated plugin.
   *
   * @return
   */
  @VisibleForTesting
  public final CompletableFuture<InterledgerFulfillPacket> getFulfillPacket() {
    return CompletableFuture.completedFuture(
      InterledgerFulfillPacket.builder()
        .data(ILP_DATA)
        .fulfillment(InterledgerFulfillment.of(PREIMAGE))
        .build()
    );
  }

  /**
   * Helper method to return the rejection packet that is used by this simulated plugin.
   *
   * @return
   */
  @VisibleForTesting
  public final InterledgerRejectPacket getSendDataRejectPacket() {
    return InterledgerRejectPacket.builder()
      .data(ILP_DATA)
      .triggeredBy(getPluginSettings().getPeerAccountAddress())
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("Handle SendData failed!")
      .build();
  }

  /**
   * Helper method to return the rejection packet that is used by this simulated plugin.
   *
   * @return
   */
  @VisibleForTesting
  public final InterledgerRejectPacket getSendMoneyRejectPacket() {
    return InterledgerRejectPacket.builder()
      .data(ILP_DATA)
      .triggeredBy(getPluginSettings().getPeerAccountAddress())
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("Handle SendMoney failed!")
      .build();
  }

  public Plugin<PluginSettings> getPluginDelegate() {
    throw new RuntimeException("Not implemented!");
  }

  @Override
  public void setPluginDelegate(Plugin<PluginSettings> pluginDelegate) {
    // This is a no-op because this is a simulated Client.
  }
}