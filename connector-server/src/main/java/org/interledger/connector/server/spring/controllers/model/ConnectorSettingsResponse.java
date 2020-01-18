package org.interledger.connector.server.spring.controllers.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableConnectorSettingsResponse.class)
public interface ConnectorSettingsResponse {

  static ImmutableConnectorSettingsResponse.Builder builder() {
    return ImmutableConnectorSettingsResponse.builder();
  }

  /**
   * The ILP Address of this connector. Note that the Connector's initial properties may not specify an address, in
   * which case the default will be {@link Link#SELF}. In this case the Connector will use IL-DCP to obtain its
   * operating address.
   *
   * @return The ILP address of this connector.
   */
  @Value.Default
  @JsonProperty("ilp_address")
  default InterledgerAddress operatorAddress() {
    return Link.SELF;
  }

String version() {
  }
}
