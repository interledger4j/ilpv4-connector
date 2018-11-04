package com.sappenin.ilpv4.plugins;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import com.sappenin.ilpv4.plugins.btp.BtpSocketHandler;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.SimulatedChildPlugin;
import org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.BtpServerPluginSettings;
import org.interledger.plugin.lpiv2.btp2.ImmutableBtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.ServerWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Objects;

import static org.interledger.plugin.lpiv2.btp2.BtpPluginSettings.KEY_SECRET;

/**
 * A factory for constructing instances of {@link org.interledger.plugin.lpiv2.Plugin} based upon configured settings.
 */
public class IlpPluginFactory {

  private final CodecContext ilpCodecContext;
  private final CodecContext btpCodecContext;
  private final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry;
  private final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;
  private final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  private final IlpPacketSwitch ilpPacketSwitch;

  // Required to connect this class's potentially-constructed server plugin to the Spring WebSocket server.
  @Autowired
  private ApplicationContext applicationContext;

  /**
   * Required-args Constructo.
   */
  public IlpPluginFactory(
    final CodecContext ilpCodecContext,
    final CodecContext btpCodecContext,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final IlpPacketSwitch ilpPacketSwitch
  ) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.btpCodecContext = Objects.requireNonNull(btpCodecContext);
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
          btpPacketToBinaryMessageConverter
        );

        // TODO: Instead of doing it this way via late-binding, we can remove the BtpSocketHandler, and always create
        // an instance of BtpServerPlugin, and Inject it into SpringWsConfig. If Websockets are turned on, we can
        // connect the plugin to the WebsocketHandler directly in the config.

        // Grab BtpSocketHandler from the Context, and call setServerWebsocketBtpPlugin(this) in order to connect the two.
        this.applicationContext.getBean(BtpSocketHandler.class).setServerWebsocketBtpPlugin(
          (ServerWebsocketBtpPlugin) constructedPlugin
        );

        break;
      }
      case ClientWebsocketBtpPlugin.PLUGIN_TYPE_STRING: {
        // Translate from Plugin.customSettings, being sure to apply custom settings from the incoming plugin.
        final ImmutableBtpClientPluginSettings.Builder builder = BtpClientPluginSettings.builder();
        final BtpClientPluginSettings clientPluginSettings =
          BtpClientPluginSettings.applyCustomSettings(builder, pluginSettings.getCustomSettings()).build();
        constructedPlugin = new ClientWebsocketBtpPlugin(
          clientPluginSettings,
          ilpCodecContext,
          btpCodecContext,
          btpSubProtocolHandlerRegistry,
          binaryMessageToBtpPacketConverter,
          btpPacketToBinaryMessageConverter,
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
