package com.sappenin.interledger.ilpv4.connector.server.spring.settings.btp.pluginMode;

import com.sappenin.interledger.ilpv4.connector.server.spring.settings.ConnectorProfile;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.plugin.lpiv2.btp2.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.AuthBtpSubprotocolHandler;
import org.interledger.plugin.lpiv2.btp2.subprotocols.auth.ClientAuthBtpSubprotocolHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.function.Supplier;

/**
 * <p>Configures a BTP server for this connector.</p>
 */
@Configuration
@Profile(ConnectorProfile.PLUGIN_MODE) // PluginMode operates a single BTP Client.
public class BtpPluginModeConfig {

  @Autowired
  Supplier<ConnectorSettings> connectorSettingsSupplier;

  // TODO: For each client, addAccount a new ClientPlugin to the Account manager and try to connect to a remote.

  @Bean
  @Profile(ConnectorProfile.DEV)
  AuthBtpSubprotocolHandler devAuthBtpSubprotocolHandler() {
    return new ClientAuthBtpSubprotocolHandler();
  }

  @Bean
  @Profile(ConnectorProfile.PROD)
  AuthBtpSubprotocolHandler prodAuthBtpSubprotocolHandler() {
    return new ClientAuthBtpSubprotocolHandler();
  }

  @Bean
  BtpSubProtocolHandlerRegistry btpSubProtocolHandlerRegistry(final AuthBtpSubprotocolHandler authBtpSubprotocolHandler) {
    return new BtpSubProtocolHandlerRegistry(authBtpSubprotocolHandler);
  }
}
