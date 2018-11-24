package com.sappenin.interledger.ilpv4.connector.plugins.connectivity;

import com.sappenin.interledger.ilpv4.connector.plugins.InternallyRoutedPlugin;
import org.interledger.core.*;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginType;
import org.interledger.plugin.lpiv2.settings.PluginSettings;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A LPIv2 {@link Plugin} that implements the <tt>ping</tt> protocol. The packet-switch will have routed packets
 * addressed to this connector's address to this plugin (via an appropriate routing table entry). Once this plugin's
 * {@link #sendData(InterledgerPreparePacket)} is called, it will merely respond with fulfill packets containing a known
 * response, and will also track the balance of payments this connector has accrued due to incoming ping-payments.
 * </p>
 * <p>
 * Note that while this plugin is tracking payments related to ping requests, this account is actually between the
 * connector and itself, so unless it makes sense for tracking purposes, settling the account balance tracked by this
 * plugin is unnecessary.
 * </p>
 */
public class PingProtocolPlugin extends InternallyRoutedPlugin implements Plugin<PluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "PING_PROTOCOL_PLUGIN";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);

  public static final InterledgerAddress SELF_DOT_PING
    = InterledgerAddress.of(InterledgerAddressPrefix.SELF.with("ping").getValue());

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = PING_PROTOCOL_FULFILLMENT.getCondition();

  // TODO: Add balance tracking via GCP service.

  /**
   * Required-args constructor.
   *
   * @param pluginSettings
   * @param oerCodecContext
   */
  public PingProtocolPlugin(final PluginSettings pluginSettings, final CodecContext oerCodecContext) {
    super(pluginSettings, oerCodecContext);
  }

  /**
   * In the current design, this plugin is called in the send-data flow by the switch. In this case, it's acting like a
   * plugin to the ping infrastructure contained in this connector.
   *
   * @param preparePacket
   *
   * @return
   */
  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> doSendData(InterledgerPreparePacket preparePacket) {
    return CompletableFuture.completedFuture(
      Optional.of(InterledgerFulfillPacket.builder()
        .fulfillment(PING_PROTOCOL_FULFILLMENT)
        .build())
    );

  }
}
