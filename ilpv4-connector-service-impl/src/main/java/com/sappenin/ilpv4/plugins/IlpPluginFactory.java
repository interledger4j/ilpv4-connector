package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import com.sappenin.ilpv4.plugins.btp.BtpClientPluginSettings;
import com.sappenin.ilpv4.plugins.btp.BtpServerPluginSettings;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BinaryMessageToBtpPacketConverter;
import com.sappenin.ilpv4.plugins.btp.spring.converters.BtpPacketToBinaryMessageConverter;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry;
import com.sappenin.ilpv4.plugins.btp.ws.ClientWebsocketBtpPlugin;
import com.sappenin.ilpv4.plugins.btp.ws.ServerWebsocketBtpPlugin;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.SimulatedChildPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Objects;

import static com.sappenin.ilpv4.plugins.btp.BtpPluginSettings.KEY_SECRET;

/**
 * A factory for constructing instances of {@link org.interledger.plugin.lpiv2.Plugin}.
 */
public class IlpPluginFactory {

  private final CodecContext ilpCodecContext;
  private final CodecContext btpCodecContext;
  //private final IlpLogUtils ilpLogUtils;
  private final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;
  private final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;
  private final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  private final IlpPacketSwitch ilpPacketSwitch;

  // Required to connect the server plugin that is constructed inside of this class to the Spring WebSocket server
  // handler.
  @Autowired
  private ApplicationContext appContext;

  /**
   * Required-args Constructo.
   */
  public IlpPluginFactory(
    final CodecContext ilpCodecContext,
    final CodecContext btpCodecContext,
    //final IlpLogUtils ilpLogUtils,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final IlpPacketSwitch ilpPacketSwitch) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.btpCodecContext = Objects.requireNonNull(btpCodecContext);
    //this.ilpLogUtils = Objects.requireNonNull(ilpLogUtils);
    this.btpSubProtocolHandlerRegistry = Objects.requireNonNull(btpSubProtocolHandlerRegistry);
    this.binaryMessageToBtpPacketConverter = Objects.requireNonNull(binaryMessageToBtpPacketConverter);
    this.btpPacketToBinaryMessageConverter = Objects.requireNonNull(btpPacketToBinaryMessageConverter);
    this.ilpPacketSwitch = ilpPacketSwitch;
  }

  /**
   * Construct a new instance of {@link Plugin} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Plugin}.
   */
  public Plugin<?> constructPlugin(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    final Plugin constructedPlugin;
    switch (pluginSettings.getPluginType().value()) {
      case SimulatedChildPlugin.PLUGIN_TYPE_STRING: {
        constructedPlugin = new SimulatedChildPlugin(pluginSettings);
        break;
      }
      case ServerWebsocketBtpPlugin.PLUGIN_TYPE_STRING: {
        // Translate from Plugin.customSettings...
        final BtpServerPluginSettings serverPluginSettings = BtpServerPluginSettings.builder()
          .from(pluginSettings)
          .secret(pluginSettings.getCustomSettings().get(KEY_SECRET).toString())
          .build();
        constructedPlugin = new ServerWebsocketBtpPlugin(
          serverPluginSettings,
          ilpCodecContext,
          btpCodecContext,
          btpSubProtocolHandlerRegistry,
          binaryMessageToBtpPacketConverter,
          btpPacketToBinaryMessageConverter,
          //this.ilpLogUtils,
          this.appContext
        );
        break;
      }
      case ClientWebsocketBtpPlugin.PLUGIN_TYPE_STRING: {
        // Translate from Plugin.customSettings...
        final BtpClientPluginSettings clientPluginSettings = BtpClientPluginSettings
          .fromPluginSettingsWithCustomSettings(pluginSettings)
          .build();
        constructedPlugin = new ClientWebsocketBtpPlugin(
          clientPluginSettings,
          ilpCodecContext,
          btpCodecContext,
          btpSubProtocolHandlerRegistry,
          binaryMessageToBtpPacketConverter,
          btpPacketToBinaryMessageConverter,
          //this.ilpLogUtils,
          new StandardWebSocketClient()
        );
        break;
      }
      default: {
        throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginSettings.getPluginType()));
      }
    }

    // Unregister both handlers, just in-case the plugin itself sets its own handler (e.g., SimulatedPlugin)
    constructedPlugin.unregisterDataHandler();
    constructedPlugin.unregisterMoneyHandler();

    // Register the plugin with the IlpPacketSwitch so that when a plugin receives an incoming packet, the
    // packet-switch is the system that actually handles it, and routes it properly to wherever it's supposed to go.
    constructedPlugin.registerDataHandler(ilpPacketSwitch::sendData);
    constructedPlugin.registerMoneyHandler(ilpPacketSwitch::sendMoney);

    return constructedPlugin;
  }
}
