package com.sappenin.interledger.ilpv4.connector.plugins.connectivity;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.PluginFactory;
import org.interledger.plugin.lpiv2.LoopbackPlugin;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
import org.interledger.plugin.lpiv2.PluginType;

import java.util.Objects;

/**
 * An implementation of {@link PluginFactory} for creating BTP Plugins.
 */
public class PingProtocolPluginFactory implements PluginFactory {

  private final CodecContext ilpCodecContext;

  public PingProtocolPluginFactory(final CodecContext ilpCodecContext) {
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
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
      case PingProtocolPlugin.PLUGIN_TYPE_STRING: {
        plugin = new PingProtocolPlugin(pluginSettings, ilpCodecContext);
        break;
      }
      //      case EchoProtocolPlugin.PLUGIN_TYPE_STRING: {
      //        plugin = new LoopbackPlugin(pluginSettings.getOperatorAddress());
      //        break;
      //      }
      default: {
        plugin = null;
      }
    }

    //        // Alert any plugin listeners that a new plugin was constructed...
    //        eventPublisher.publishEvent(PluginConstructedEvent.builder()
    //          .message(String.format("Plugin constructed for `%s`", pluginSettings.getAccountAddress().getValue()))
    //          .object(plugin)
    //          .build());

    return plugin;

  }

  @Override
  public boolean supports(PluginType pluginType) {
    return PingProtocolPlugin.PLUGIN_TYPE.equals(pluginType);
  }

}
