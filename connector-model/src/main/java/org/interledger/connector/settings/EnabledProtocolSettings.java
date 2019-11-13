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
   * Whether this Connector can handle ILDCP Config packets addressed to `peer.config` per RFC-TODO.
   *
   * @see "RFC-LINK"
   */
  default boolean isPeerConfigEnabled() {
    return true;
  }

  /**
   * Whether this Connector can supports IL-DCP in order to obtain its operator address.
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md"
   */
  default boolean isIldcpEnabled() {
    return false;
  }

  /**
   * Whether this Connector can handle CCP Routing packets addressed to `peer.routing` per RFC-TODO.
   *
   * @see "RFC-LINK"
   */
  default boolean isPeerRoutingEnabled() {
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
    public boolean isPeerConfigEnabled() {
      return true;
    }

    @Override
    @Value.Default
    public boolean isPeerRoutingEnabled() {
      return false;
    }

    @Override
    @Value.Default
    public boolean isIldcpEnabled() {
      return false;
    }
  }

}
