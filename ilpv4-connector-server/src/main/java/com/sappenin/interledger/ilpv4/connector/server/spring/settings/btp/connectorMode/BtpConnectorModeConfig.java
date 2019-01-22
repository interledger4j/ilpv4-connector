package com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.connectorMode;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.accounts.BtpAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.btp.BtpResponsePacket;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.plugin.lpiv2.ImmutableDefaultPluginSettings;
import org.interledger.plugin.lpiv2.btp2.spring.BtpConnectedPluginsManager;
import org.interledger.plugin.lpiv2.btp2.spring.PendingResponseManager;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BinaryMessageToBtpPacketConverter;
import org.interledger.plugin.lpiv2.btp2.spring.converters.BtpPacketToBinaryMessageConverter;
import org.interledger.plugin.lpiv2.btp2.spring.factories.BtpServerPluginFactory;
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
import static com.sappenin.interledger.ilpv4.connector.server.spring.settings.CodecContextConfig.ILP;
import static org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP;

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
    final BtpServerPluginFactory btpServerPluginFactory,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final ServerAuthBtpSubprotocolHandler serverAuthBtpSubprotocolHandler,
    final EventBus eventBus
  ) {

    // TODO: Load custom settings from property file for DefaultPluginSettings....e.g., sendMoney timeout.

    final BtpConnectedPluginsManager btpConnectedPluginsManager = new BtpConnectedPluginsManager(
      () -> connectorSettingsSupplier.get().getOperatorAddress(),
      ImmutableDefaultPluginSettings.builder().build(),
      btpServerPluginFactory,
      binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter,
      serverAuthBtpSubprotocolHandler,
      eventBus
    );

    return new LoggingWebSocketHandlerDecorator(btpConnectedPluginsManager);
  }

  ///////////////////
  // BTP Server Beans
  ///////////////////

  @Bean
  BtpServerPluginFactory btpServerPluginFactory(
    @Qualifier(ILP) final CodecContext ilpCodecContext,
    final BinaryMessageToBtpPacketConverter binaryMessageToBtpPacketConverter,
    final BtpPacketToBinaryMessageConverter btpPacketToBinaryMessageConverter,
    final BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry,
    final PendingResponseManager<BtpResponsePacket> pendingResponseManager
  ) {
    return new BtpServerPluginFactory(
      ilpCodecContext,
      binaryMessageToBtpPacketConverter,
      btpPacketToBinaryMessageConverter,
      btpSubProtocolHandlerRegistry,
      pendingResponseManager
    );
  }

  @Bean
  @Profile(ConnectorProfile.DEV)
  AuthBtpSubprotocolHandler devAuthBtpSubprotocolHandler() {
    return new ServerAuthBtpSubprotocolHandler(
      new BtpMultiAuthenticator.AlwaysAllowedBtpMultiAuthenticator(
        () -> connectorSettingsSupplier.get().getOperatorAddress())
    );
  }

  @Bean
  @Profile(ConnectorProfile.PROD)
  AuthBtpSubprotocolHandler prodAuthBtpSubprotocolHandler() {
    throw new RuntimeException("Implement real server auth mechanism!");
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry(
    final @Qualifier(ILP) CodecContext ilpCodecContext,
    final AuthBtpSubprotocolHandler authBtpSubprotocolHandler,
    final ILPv4PacketSwitch ilpv4PacketSwitch,
    final BtpAccountIdResolver btpAccountIdResolver
  ) {
    final BtpSubProtocolHandlerRegistry handlerRegistry =
      new BtpSubProtocolHandlerRegistry(authBtpSubprotocolHandler);

    // Replace the default ILP BTP Subprotocol Handler with the ConnectorMode version.
    handlerRegistry.putHandler(
      BTP_SUB_PROTOCOL_ILP,
      BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM,
      new ConnectorModeIlpBtpSubprotocolHandler(ilpCodecContext,
        btpAccountIdResolver,
        ilpv4PacketSwitch
      )
    );

    return handlerRegistry;
  }
}
