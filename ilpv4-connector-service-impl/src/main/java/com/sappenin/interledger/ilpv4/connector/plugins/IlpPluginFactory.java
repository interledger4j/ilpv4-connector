package com.sappenin.interledger.ilpv4.connector.plugins;

import com.sappenin.interledger.ilpv4.connector.model.events.PluginConstructedEvent;
import com.sappenin.interledger.ilpv4.connector.plugins.connectivity.PingProtocolPlugin;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.link.mux.PluginMux;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.SimulatedPlugin;
import org.interledger.plugin.lpiv2.btp2.BtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.BtpServerPluginSettings;
import org.interledger.plugin.lpiv2.btp2.ImmutableBtpClientPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.AbstractWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.BtpBinaryWebSocketHandler;
import org.interledger.plugin.lpiv2.btp2.spring.ClientWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.ServerWebsocketBtpPlugin;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpAuthenticationService;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.ilp.IlpBtpSubprotocolHandler;
import org.interledger.plugin.lpiv2.settings.PluginSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Objects;

/**
 * A factory for constructing instances of {@link org.interledger.plugin.lpiv2.Plugin} based upon configured settings.
 */
public class IlpPluginFactory {

  private final CodecContext ilpCodecContext;
  private final CodecContext btpCodecContext;
  private final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter;
  private final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter;

  private final ApplicationEventPublisher eventPublisher;
  private final BtpAuthenticationService btpAuthenticationService;

  // Required to connect this class's potentially-constructed server plugin to the Spring WebSocket server.
  @Autowired
  private ApplicationContext applicationContext;

  /**
   * Required-args Constructo.
   */
  public IlpPluginFactory(
    final CodecContext ilpCodecContext,
    final CodecContext btpCodecContext,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final BtpAuthenticationService btpAuthenticationService,
    final ApplicationEventPublisher eventPublisher
  ) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.btpCodecContext = Objects.requireNonNull(btpCodecContext);
    this.binaryMessageToBtpPacketConverter = Objects.requireNonNull(binaryMessageToBtpPacketConverter);
    this.btpPacketToBinaryMessageConverter = Objects.requireNonNull(btpPacketToBinaryMessageConverter);
    this.eventPublisher = Objects.requireNonNull(eventPublisher);
    this.btpAuthenticationService = Objects.requireNonNull(btpAuthenticationService);
  }

  /**
   * Construct a new instance of {@link Plugin} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Plugin}.
   */
  public Plugin<?> constructPlugin(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    final Plugin<?> plugin;
    switch (pluginSettings.getPluginType().value()) {
      case SimulatedPlugin.PLUGIN_TYPE_STRING: {
        plugin = new SimulatedPlugin(pluginSettings);
        break;
      }
      case ServerWebsocketBtpPlugin.PLUGIN_TYPE_STRING: {
        plugin = this.constructServerWebsocketBtpPlugin(pluginSettings);

        // Register this plugin as an eligible plugin for incoming Websocket sessions, but only if the incoming
        // WebSocketSession has authenticated with the authUsername.
        final InterledgerAddress authUsername = ((BtpServerPluginSettings) plugin.getPluginSettings()).getAuthUsername()
          .map(InterledgerAddress::of)
          .orElseThrow(() -> new RuntimeException("AuthUserName must be set on a Server Plugin!"));

        final PluginMux<AbstractWebsocketBtpPlugin<? extends BtpServerPluginSettings>>
          pluginMux = this.applicationContext.getBean(BtpBinaryWebSocketHandler.class).getPluginMux();
        pluginMux.registerPlugin(authUsername, (ServerWebsocketBtpPlugin) plugin);
        break;
      }
      case ClientWebsocketBtpPlugin.PLUGIN_TYPE_STRING: {
        plugin = this.constructClientWebsocketBtpPlugin(pluginSettings);
        break;
      }
      case PingProtocolPlugin.PLUGIN_TYPE_STRING: {
        plugin = this.constructPingProtocolPlugin(pluginSettings);
        break;
      }
      default: {
        throw new RuntimeException(String.format("Unsupported PluginType: %s", pluginSettings.getPluginType()));
      }
    }

    // Alert any plugin listeners that a new plugin was constructed...
    eventPublisher.publishEvent(PluginConstructedEvent.builder()
      .message(String.format("Plugin constructed for `%s`", pluginSettings.getPeerAccountAddress().getValue()))
      .object(plugin)
      .build());

    return plugin;
  }

  /////////////////
  // Helper Methods
  /////////////////

  /**
   * Construct a new plugin to handle all BTP requests using a Websocket server.
   *
   * @param pluginSettings An instance of {@link PluginSettings} that can be used to configure the newly constructed
   *                       plugin.
   *
   * @return A newly constructed instance of {@link ServerWebsocketBtpPlugin}.
   */
  private ServerWebsocketBtpPlugin constructServerWebsocketBtpPlugin(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry =
      new BtpSubProtocolHandlerRegistry(btpAuthenticationService);
    final IlpBtpSubprotocolHandler ilpBtpSubprotocolHandler = new IlpBtpSubprotocolHandler();
    btpSubProtocolHandlerRegistry.putHandler(
      BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP,
      BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM,
      ilpBtpSubprotocolHandler
    );

    // Translate from Plugin.customSettings into typed BtpServerPluginSettings...
    final BtpServerPluginSettings serverPluginSettings = BtpServerPluginSettings.
      applyCustomSettings(
        BtpServerPluginSettings.builder().from(pluginSettings), pluginSettings.getCustomSettings()
      )
      .build();

    return new ServerWebsocketBtpPlugin(
      serverPluginSettings,
      ilpCodecContext,
      btpCodecContext,
      btpSubProtocolHandlerRegistry,
      binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter
    );
  }

  /**
   * Construct a new plugin to handle all BTP requests using a Websocket client.
   *
   * @param pluginSettings An instance of {@link PluginSettings} that can be used to configure the newly constructed
   *                       plugin.
   *
   * @return A newly constructed instance of {@link ClientWebsocketBtpPlugin}.
   */
  private ClientWebsocketBtpPlugin constructClientWebsocketBtpPlugin(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    // Construct a new Registry and IlpHandler...
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry = new BtpSubProtocolHandlerRegistry(
      new BtpAuthenticationService.NoOpBtpAuthenticationService()
    );
    final IlpBtpSubprotocolHandler ilpBtpSubprotocolHandler = new IlpBtpSubprotocolHandler();
    btpSubProtocolHandlerRegistry.putHandler(
      BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP,
      BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM,
      ilpBtpSubprotocolHandler
    );

    // Translate from Plugin.customSettings, being sure to apply custom settings from the incoming plugin.
    final ImmutableBtpClientPluginSettings.Builder builder = BtpClientPluginSettings.builder();
    final BtpClientPluginSettings clientPluginSettings =
      BtpClientPluginSettings.applyCustomSettings(builder, pluginSettings.getCustomSettings()).build();

    return new ClientWebsocketBtpPlugin(
      clientPluginSettings,
      ilpCodecContext,
      btpCodecContext,
      btpSubProtocolHandlerRegistry,
      binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter,
      new StandardWebSocketClient()
    );
  }

  /**
   * Construct a new plugin to handle all ping-protocol operations.
   *
   * @param pluginSettings An instance of {@link PluginSettings} that can be used to configure the newly constructed
   *                       plugin.
   *
   * @return A newly constructed instance of {@link PingProtocolPlugin}.
   */
  private PingProtocolPlugin constructPingProtocolPlugin(final PluginSettings pluginSettings) {
    Objects.requireNonNull(pluginSettings);

    return new PingProtocolPlugin(pluginSettings, ilpCodecContext);
  }
}
