package com.sappenin.interledger.ilpv4.connector.plugins.connectivity;

import com.sappenin.interledger.ilpv4.connector.plugins.InternallyRoutedPlugin;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>A {@link Plugin} that implements the <tt>PING</tt> protocol. The packet-switch will have routed packets
 * addressed to this connector's operating address to this plugin (via an appropriate routing table entry). Once this
 * plugin is called, it will merely respond with fulfill packets containing a known response, and will also track the
 * balance of payments this connector has accrued due to incoming ping-payments.</p>
 *
 * <p>Note that while this plugin is tracking payments related to ping requests, this account is actually between the
 * connector and itself, so unless it makes sense for tracking purposes, settling the account balance tracked by this
 * plugin is unnecessary.</p>
 *
 * // TODO: Add RFC link.
 *
 * @see ""
 */
public class PingProtocolPlugin extends InternallyRoutedPlugin implements Plugin<PluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "PING_PROTOCOL_PLUGIN";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);

//  public static final InterledgerAddress SELF_DOT_PING
//    = InterledgerAddress.of(InterledgerAddressPrefix.SELF.with("ping").getValue());

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = PING_PROTOCOL_FULFILLMENT.getCondition();

  // TODO: Add balance tracking?

  /**
   * Required-args constructor.
   */
  public PingProtocolPlugin(final PluginSettings pluginSettings, final CodecContext oerCodecContext) {
    super(pluginSettings, oerCodecContext);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> sendData(final InterledgerPreparePacket preparePacket) {

    Objects.requireNonNull(preparePacket);

    final InterledgerResponsePacket responsePacket;
    if (preparePacket.getExecutionCondition().equals(PING_PROTOCOL_CONDITION)) {
      responsePacket = InterledgerFulfillPacket.builder()
        .fulfillment(PING_PROTOCOL_FULFILLMENT)
        .data(preparePacket.getData())
        .build();
    } else {
      responsePacket = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .message("Invalid Ping Protocol Condition")
        .triggeredBy(getPluginSettings().getOperatorAddress())
        .build();
    }

    return CompletableFuture.completedFuture(Optional.of(responsePacket));
  }
}
