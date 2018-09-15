package com.sappenin.ilpv4.model.settings;

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

  /**
   * @return The ILP address of this connector.
   */
  InterledgerAddress getIlpAddress();

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
  default RoutingSettings getRoutingSettings() {
    return ImmutableRoutingSettings.builder().build();
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
    return this.getAccounts().stream()
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
  List<? extends AccountSettings> getAccounts();

  /**
   * The global address prefix for this operating envionment.
   */
  @Value.Default
  default InterledgerAddressPrefix getGlobalPrefix() {
    return InterledgerAddressPrefix.of("g");
  }

}
