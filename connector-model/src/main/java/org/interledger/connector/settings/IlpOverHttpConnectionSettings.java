package org.interledger.connector.settings;

import org.immutables.value.Value;

@Value.Immutable(intern = true)
@Value.Modifiable
public interface IlpOverHttpConnectionSettings {

  static ImmutableIlpOverHttpConnectionSettings.Builder builder() {
    return ImmutableIlpOverHttpConnectionSettings.builder();
  }

  @Value.Default
  default long connectTimeoutMillis() {
    return 1000;
  }

  @Value.Default
  default long readTimeoutMillis() {
    return 60000;
  }

  @Value.Default
  default long writeTimeoutMillis() {
    return 60000;
  }

  @Value.Default
  default int maxRequests() {
    return 100;
  }

  @Value.Default
  default int maxRequestsPerHost() {
   return 50;
  }

  @Value.Default
  default int maxIdleConnections() {
   return 10;
  }

  @Value.Default
  default long keepAliveSeconds() {
    return 30;
  }
}
