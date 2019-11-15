package org.interledger.connector.settings;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.Link;

import org.immutables.value.Value;

import java.util.Objects;

/**
 * A view of the settings currently configured for this Connector.
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface ConnectorSettings {

  String OVERRIDE_BEAN_NAME = "interledger.connector.connectorSettingsOverride";

  /**
   * The ILP Address of this connector. Note that the Connector's initial properties may not specify an address, in
   * which case the default will be {@link Link#SELF}. In this case the Connector will use IL-DCP to obtain its
   * operating address.
   *
   * @return The ILP address of this connector.
   */
  @Value.Default
  default InterledgerAddress operatorAddress() {
    return Link.SELF;
  }

  /**
   * The global address prefix for this operating environment.
   */
  @Value.Default
  default InterledgerAddressPrefix globalPrefix() {
    return InterledgerAddressPrefix.GLOBAL;
  }

  @Value.Default
  default EnabledProtocolSettings enabledProtocols() {
    return EnabledProtocolSettings.builder().build();
  }

  @Value.Default
  default EnabledFeatureSettings enabledFeatures() {
    return EnabledFeatureSettings.builder().build();
  }

  @Value.Default
  default boolean websocketServerEnabled() {
    return false;
  }

  /**
   * Which account should be used as the default route for all un-routed traffic. If empty, the default route is
   * disabled.
   */
  @Value.Default
  default GlobalRoutingSettings globalRoutingSettings() {
    return ImmutableGlobalRoutingSettings.builder().build();
  }

  /**
   * Convert a child account into an address scoped underneath this connector. For example, given an input address,
   * append it to this connector's address to create a child address that this Connector can advertise as its own.
   *
   * @param childAccountId The {@link AccountId} of a child account.
   * @return An {@link InterledgerAddress } representing the new address of the supplied child account.
   */
  default InterledgerAddress toChildAddress(final AccountId childAccountId) {
    Objects.requireNonNull(childAccountId);
    return this.operatorAddress().with(childAccountId.value());
  }

  /**
   * The minimum time the connector wants to budget for getting a message to the accounts its trading on. Budget is
   * mainly to cover the latency to send the fulfillment packet to the downstream node.
   *
   * @return minimum message window time in milliseconds
   */
  @Value.Default
  default int minMessageWindowMillis() {
    return 1000;
  }

  /**
   * The amount of time that Connector will wait around for a fulfillment/rejection. This is equivalent to outgoing link
   * timeout duration.
   *
   * @return max hold time in milliseconds
   */
  @Value.Default
  default int maxHoldTimeMillis() {
    return 30000;
  }


  /**
   * Flag to control if shared secrets must be 32 bytes
   * @return true if required otherwise anything goes
   */
  @Value.Default
  default boolean isRequire32ByteSharedSecrets() {
    return false;
  };

  /**
   * Keys the connector will use for various core functions.
   *
   * @return keys
   */
  @Value.Default
  default ConnectorKeys keys() {
    return ConnectorKeys.builder()
      .accountSettings(ConnectorKey.builder().alias("accountSettings").version("1").build())
      .secret0(ConnectorKey.builder().alias("secret0").version("1").build())
      .build();
  }

}
