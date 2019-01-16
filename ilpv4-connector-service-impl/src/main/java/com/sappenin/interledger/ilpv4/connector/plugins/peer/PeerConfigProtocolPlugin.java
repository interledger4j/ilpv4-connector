package com.sappenin.interledger.ilpv4.connector.plugins.peer;

import com.sappenin.interledger.ilpv4.connector.plugins.InternallyRoutedPlugin;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerRuntimeException;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.sappenin.interledger.ilpv4.connector.ccp.CcpConstants.PEER_PROTOCOL_EXECUTION_CONDITION;

/**
 * <p>A LPIv2 {@link Plugin} that implements the ILP Peer Config sub-protocol, which allows a Connector to support
 * IL-DCP in order to provide a child-plugin an Interledger address rooted in this Connector's namespace.</p>
 *
 * <p>This plugin functions like the `ildcpHostController` in the Javascript implementation.</p>
 */
public class PeerConfigProtocolPlugin extends InternallyRoutedPlugin implements Plugin<PluginSettings> {

  public static final String PLUGIN_TYPE_STRING = "PEER_CONFIG_PLUGIN";
  public static final PluginType PLUGIN_TYPE = PluginType.of(PLUGIN_TYPE_STRING);

  /**
   * Required-args Constructor.
   *
   * @param pluginSettings  A {@link PluginSettings} that specifies all plugin options.
   * @param oerCodecContext
   */
  public PeerConfigProtocolPlugin(final PluginSettings pluginSettings, final CodecContext oerCodecContext) {
    super(pluginSettings, oerCodecContext);
  }

  @Override
  public CompletableFuture<Optional<InterledgerResponsePacket>> sendData(InterledgerPreparePacket preparePacket) {
    // TODO
    throw new RuntimeException("FIXME!");
  }

  //  @Override
//  public CompletableFuture<Optional<InterledgerResponsePacket>> doSendData(InterledgerPreparePacket preparePacket) {
//
//    if (!PEER_PROTOCOL_EXECUTION_CONDITION.equals(preparePacket.getExecutionCondition())) {
//      throw new InterledgerRuntimeException("Packet does not contain correct condition for a peer protocol request.");
//    }
//
//    throw new RuntimeException("FIXME! See ildcpHostController");
//
//  }
}
