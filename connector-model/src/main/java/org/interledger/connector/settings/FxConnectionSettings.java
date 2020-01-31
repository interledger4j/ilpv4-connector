package org.interledger.connector.settings;

import org.immutables.value.Value;

@Value.Immutable(intern = true)
@Value.Modifiable
public interface FxConnectionSettings {

  static ImmutableFxConnectionSettings.Builder builder() {
    return ImmutableFxConnectionSettings.builder();
  }

  @Value.Default
  default int maxIdleConnections() {
    return 5;
  }

  @Value.Default
  default long keepAliveMinutes() {
    return 1;
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
}

