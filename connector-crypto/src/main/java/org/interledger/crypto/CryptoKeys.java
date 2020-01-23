package org.interledger.crypto;


import org.immutables.value.Value;

/**
 * Named keys that are configured for use by the connector.
 */
@Value.Immutable
public interface CryptoKeys {

  static ImmutableCryptoKeys.Builder builder() {
    return ImmutableCryptoKeys.builder();
  }

  /**
   * Core secret for the connector
   *
   * @return key
   */
  CryptoKey secret0();

  /**
   * Key for incoming/outgoing shared secrets on account settings
   * @return key
   */
  CryptoKey accountSettings();

}
