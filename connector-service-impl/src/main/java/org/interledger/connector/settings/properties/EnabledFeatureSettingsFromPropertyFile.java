package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.EnabledFeatureSettings;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class EnabledFeatureSettingsFromPropertyFile implements EnabledFeatureSettings {

  private boolean rateLimitingEnabled;
  private boolean require32ByteSharedSecrets;
  private boolean localSpspFulfillmentEnabled;
  private StreamPaymentAggregationMode streamPaymentAggregationMode;

  @Override
  public boolean isRateLimitingEnabled() {
    return rateLimitingEnabled;
  }

  public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
    this.rateLimitingEnabled = rateLimitingEnabled;
  }

  @Override
  public boolean isRequire32ByteSharedSecrets() {
    return require32ByteSharedSecrets;
  }

  public void setRequire32ByteSharedSecrets(boolean require32ByteSharedSecrets) {
    this.require32ByteSharedSecrets = require32ByteSharedSecrets;
  }

  @Override
  public boolean isLocalSpspFulfillmentEnabled() {
    return localSpspFulfillmentEnabled;
  }

  public void setLocalSpspFulfillmentEnabled(boolean localSpspFulfillmentEnabled) {
    this.localSpspFulfillmentEnabled = localSpspFulfillmentEnabled;
  }

  @Override
  public StreamPaymentAggregationMode streamPaymentAggregationMode() {
    return streamPaymentAggregationMode;
  }

  public StreamPaymentAggregationMode getStreamPaymentAggregationMode() {
    return streamPaymentAggregationMode;
  }

  public void setStreamPaymentAggregationMode(StreamPaymentAggregationMode streamPaymentAggregationMode) {
    this.streamPaymentAggregationMode = streamPaymentAggregationMode;
  }
}
