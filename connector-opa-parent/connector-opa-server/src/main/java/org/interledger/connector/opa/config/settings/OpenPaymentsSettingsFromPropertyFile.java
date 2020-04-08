package org.interledger.connector.opa.config.settings;

import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "interledger.connector.open-payments")
public class OpenPaymentsSettingsFromPropertyFile implements OpenPaymentsSettings {

  private InterledgerAddress ilpOperatorAddress = Link.SELF;

  private OpenPaymentsMetadataFromPropertyFile metadata = new OpenPaymentsMetadataFromPropertyFile();

  @Override
  public InterledgerAddress ilpOperatorAddress() {
    return ilpOperatorAddress;
  }

  @Override
  public OpenPaymentsMetadata metadata() {
    return metadata;
  }

  public void setIlpOperatorAddress(InterledgerAddress ilpOperatorAddress) {
    this.ilpOperatorAddress = ilpOperatorAddress;
  }

  public void setMetadata(OpenPaymentsMetadataFromPropertyFile metadata) {
    this.metadata = metadata;
  }
}
