package com.sappenin.interledger.ilpv4.connector.model.settings;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.List;

/**
 * A view of the settings currently configured for this Connector.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface ConnectorSettings {

  String BEAN_NAME = "connectorSettings";
  String PROPERTY_NAME__WEBSOCKETS_ENABLED = "ilpv4.connector.websocketServerEnabled";

  /**
   * @return The ILP address of this connector.
   */
  InterledgerAddress getIlpAddress();

  /**
   * The global address prefix for this operating envionment.
   */
  @Value.Default
  default InterledgerAddressPrefix getGlobalPrefix() {
    return InterledgerAddressPrefix.of("g");
  }

  @Value.Default
  default boolean websocketServerEnabled() {
    return false;
  }

  /**
   * Defines route-update settings for the Connector globally.
   *
   * @return An instance of {@link RouteBroadcastSettings}.
   */
  @Value.Default
  default RouteBroadcastSettings getRouteBroadcastSettings() {
    return ImmutableRouteBroadcastSettings.builder().build();
  }

  /**
   * Which account should be used as the default route for all un-routed traffic. If empty, the default route is
   * disabled.
   */
  @Value.Default
  default DefaultRouteSettings getDefaultRouteSettings() {
    return ImmutableDefaultRouteSettings.builder().build();
  }

  /**
   * Accessor for the specific {@link RouteBroadcastSettings} for the specified peer address.
   *
   * @param peerAccountAddress The ILP address of the remote peer to obtain broadcast settings for.
   *
   * @return An instance of {@link RouteBroadcastSettings} if configured, or else {@link #getRouteBroadcastSettings()}.
   */
  @Value.Default
  default RouteBroadcastSettings getRouteBroadcastSettings(final InterledgerAddress peerAccountAddress) {
    return this.getAccountSettings().stream()
      .filter(accountSettings -> accountSettings.getInterledgerAddress().equals(peerAccountAddress))
      .findFirst()
      .map(AccountSettings::getRouteBroadcastSettings)
      .orElseGet(this::getRouteBroadcastSettings);
  }

  /**
   * Contains settings for all accounts configured for this Connector.
   *
   * @return An Collection of type {@link AccountSettings}.
   */
  List<AccountSettings> getAccountSettings();
}
