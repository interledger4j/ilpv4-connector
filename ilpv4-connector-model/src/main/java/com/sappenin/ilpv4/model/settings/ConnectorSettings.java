package com.sappenin.ilpv4.model.settings;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Collection;

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
   * @return
   */
  String getSecret();

  /**
   * Defines route-update settings for the Connector globally.
   *
   * @return An instance of {@link RouteBroadcastSettings}.
   */
  @Value.Default
  default RouteBroadcastSettings getRouteBroadcastSettings() {
    return ModifiableRouteBroadcastSettings.create();
  }

  /**
   * Contains settins for all accounts and associated plugins.
   *
   * @return An instance of {@link AccountSettings}.
   */
  Collection<? extends AccountSettings> getAccountSettings();

}
