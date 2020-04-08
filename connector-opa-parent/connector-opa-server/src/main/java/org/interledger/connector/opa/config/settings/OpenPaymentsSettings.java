package org.interledger.connector.opa.config.settings;

import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;

import org.immutables.value.Value;

@Value.Immutable(intern = true)
public interface OpenPaymentsSettings {

  @Value.Default
  default InterledgerAddress ilpOperatorAddress() {
    return Link.SELF;
  }


  @Value.Default
  default OpenPaymentsMetadata metadata() {
    return OpenPaymentsMetadata.builder().build();
  }
}
