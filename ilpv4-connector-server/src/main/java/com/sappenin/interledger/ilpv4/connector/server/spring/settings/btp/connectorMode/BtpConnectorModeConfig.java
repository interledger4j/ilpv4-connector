package com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.connectorMode;

import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.btp2.spring.BtpConnectedPluginsManager;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpMultiAuthenticator;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.AuthBtpSubprotocolHandler;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.ServerAuthBtpSubprotocolHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.BTP;

/**
 * <p>Configures BTP for a Connector running in `connector-mode`. This type of Connector supports a single BTP
 * Server in addition to multiple, statically configured, BTP Client plugins.</p>
 */
@Configuration
@Profile(ConnectorProfile.CONNECTOR_MODE)
@Import({SpringWebsocketServerConfig.class}) // Only run a websocket server in `CONNECTOR_MODE` mode.
public class BtpConnectorModeConfig {

  @Autowired
  Supplier<ConnectorSettings> connectorSettingsSupplier;

  @Autowired
  @Qualifier(BTP)
  CodecContext codecContext;

  @Bean
  WebSocketHandler btpWebSocketHandler(
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry
  ) {

    // TODO: Load custom settings from property file for DefaultPluginSettings....e.g., sendMoney timeout.

    //    final InterledgerAddress operatorAddress,
    //    final DefaultPluginSettings defaultPluginSettings,
    //    final PluginFactory pluginFactory,
    //    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    //    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    //    final ServerAuthBtpSubprotocolHandler authBtpSubprotocolHandler


    BtpConnectedPluginsManager btpConnectedPluginsManager = new BtpConnectedPluginsManager(
      () -> connectorSettingsSupplier.get().getOperatorAddress()
      );


    //    final BtpConnectedPluginsManager btpConnectedPluginsManager = new BtpConnectedPluginsManager(
    //      connectorSettingsSupplier.get().getOperatorAddress(),
    //      ImmutableDefaultPluginSettings.builder().build(),
    //      btpServerPluginFactory,
    //      binaryMessageToBtpPacketConverter,
    //      btpPacketToBinaryMessageConverter,
    //      btpServerPluginFactory
    //    );

    // Create a new instance of BtpSocketHandler for listening to incoming connection on a configured port...
    //        final SingleAccountBtpServerConnection actual = new SingleAccountBtpServerConnection(
    //          connectorSettingsSupplier.getOperatorAddress(),
    //          getAccountAddress(),
    //          serverBtpWebsocketMux
    //        );

    return new LoggingWebSocketHandlerDecorator(btpConnectedPluginsManager);

  }

  ///////////////////
  // BTP Server Beans
  ///////////////////

  @Bean
  @Profile(ConnectorProfile.DEV)
  AuthBtpSubprotocolHandler devAuthBtpSubprotocolHandler() {
    return new ServerAuthBtpSubprotocolHandler(
      new BtpMultiAuthenticator.AlwaysAllowedBtpMultiAuthenticator(connectorSettingsS.getOperatorAddress())
    );
  }

  @Bean
  @Profile(ConnectorProfile.PROD)
  AuthBtpSubprotocolHandler prodAuthBtpSubprotocolHandler() {
    throw new RuntimeException("Implement real server auth mechanism!");
  }

}
