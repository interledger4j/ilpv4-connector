package com.sappenin.interledger.ilpv4.connector.settings;

import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountProviderSettings;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.List;
import java.util.Optional;

/**
 * A view of the settings currently configured for this Connector.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface ConnectorSettings {

  String OVERRIDE_BEAN_NAME = "ilpv4.connector.connectorSettingsOverride";
  String BEAN_NAME = "ilpv4.connector.connectorSettings";

  /**
   * The ILP Address of this connector. Note that this may be `empty` during startup, in which case the Connector will
   * use IL-DCP to obtain its operating address.
   *
   * @return The ILP address of this connector.
   */
  Optional<InterledgerAddress> getOperatorAddress();

  /**
   * Obtain the ILP address or throw an exception.
   *
   * @return The ILP address of this connector, which will never be null.
   */
  default InterledgerAddress getOperatorAddressSafe() {
    return this.getOperatorAddress().get();
  }

  /**
   * The global address prefix for this operating environment.
   */
  @Value.Default
  default InterledgerAddressPrefix getGlobalPrefix() {
    return InterledgerAddressPrefix.of("test");
  }

  @Value.Default
  default EnabledProtocolSettings getEnabledProtocols() {
    return EnabledProtocolSettings.builder().build();
  }

  @Value.Default
  default EnabledFeatureSettings getEnabledFeatures() {
    return EnabledFeatureSettings.builder().build();
  }

  @Value.Default
  default boolean websocketServerEnabled() {
    return false;
  }

  /**
   * The JWT token issuer used for all HTTP endpoints.
   *
   * @return
   *
   * @deprecated This value will be removed in a future release and moved into an HTTP settings object.
   */
  @Deprecated
  @Value.Default
  default HttpUrl getJwtTokenIssuer() {
    // This is fine as a default. If BLAST is enabled, then this will be set overtly. If BLAST is disabled, then this
    // setting is unused.
    return HttpUrl.parse("https://example.com");
  }

  /**
   * Which account should be used as the default route for all un-routed traffic. If empty, the default route is
   * disabled.
   */
  @Value.Default
  default GlobalRoutingSettings getGlobalRoutingSettings() {
    return ImmutableGlobalRoutingSettings.builder().build();
  }

  /**
   * Convert a child account into an address scoped underneath this connector. For example, given an input address,
   * append it to this connector's address to create a child address that this Connector can advertise as its own.
   *
   * @param childAccountId The {@link AccountId} of a child account.
   *
   * @return An {@link InterledgerAddress } representing the new address of the supplied child account.
   */
  default InterledgerAddress toChildAddress(AccountId childAccountId) {
    return this.getOperatorAddressSafe().with(childAccountId.value());
  }
}
