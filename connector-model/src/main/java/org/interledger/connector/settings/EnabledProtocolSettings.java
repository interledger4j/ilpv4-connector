package org.interledger.connector.settings;

import org.immutables.value.Value;

/**
 * Indicates which protocols should be enabled or disabled in this Connector.
 */
public interface EnabledProtocolSettings {

  static ImmutableEnabledProtocolSettings.Builder builder() {
    return ImmutableEnabledProtocolSettings.builder();
  }

  /**
   * Whether this Connector can handle `ILP-over-HTTP` packets per IL-RFC-35.
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
   */
  default boolean isIlpOverHttpEnabled() {
    return true;
  }

  /**
   * Whether this Connector can handle `PING` packets per RFC-TODO.
   *
   * @see "RFC-LINK"
   */
  default boolean isPingProtocolEnabled() {
    return true;
  }

  /**
   * Whether this Connector can supports IL-DCP in order to obtain its operator address.
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md"
   */
  default boolean isIldcpEnabled() {
    return true;
  }

  /**
   * Whether this Connector can handle CCP Routing packets addressed to `peer.routing` per RFC-TODO.
   *
   * @see "RFC-LINK"
   */
  default boolean isPeerRoutingEnabled() {
    return true;
  }

  /**
   * <p>A configuration property that enables SPSP API support in the Connector. When enabled, the connector will
   * respond to SPSP requests at any root URL-path (i.e., requests that include an Accept header containing
   * `application/spsp4+json`) and process them as requests for SPSP connection information.</p>
   *
   * <p>Note that it is possible to operate an SPSP server outside of the Connector while still fulfilling STREAM
   * payments locally by setting this value to {@code false} and {@link EnabledFeatureSettings#isLocalSpspFulfillmentEnabled()}
   * to {@code true}.</p>
   *
   * @return {@code true} if the SPSP server endpoint should be enabled in this Connector; {@code false} otherwise.
   */
  default boolean isSpspEnabled() {
    return false;
  }

  @Value.Immutable(intern = true)
  abstract class AbstractEnabledProtocolSettings implements EnabledProtocolSettings {

    @Override
    @Value.Default
    public boolean isIlpOverHttpEnabled() {
      return true;
    }

    @Override
    @Value.Default
    public boolean isPingProtocolEnabled() {
      return true;
    }

    @Override
    @Value.Default
    public boolean isPeerRoutingEnabled() {
      return true;
    }

    @Override
    @Value.Default
    public boolean isIldcpEnabled() {
      return true;
    }
  }

}
