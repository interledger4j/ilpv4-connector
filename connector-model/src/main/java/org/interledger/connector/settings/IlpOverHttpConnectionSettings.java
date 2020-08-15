package org.interledger.connector.settings;

import org.immutables.value.Value;

/**
 * Connection settings used to configure an {@link okhttp3.OkHttpClient} for use in IlpOverHttp operations,
 * including configuration properties for a {@link okhttp3.ConnectionPool}
 */
@Value.Immutable(intern = true)
@Value.Modifiable
public interface IlpOverHttpConnectionSettings {

  static ImmutableIlpOverHttpConnectionSettings.Builder builder() {
    return ImmutableIlpOverHttpConnectionSettings.builder();
  }

  /**
   * Defines the maximum number of idle connections in a {@link okhttp3.ConnectionPool}
   *
   * @return number of idle connections
   */
  @Value.Default
  default int maxIdleConnections() {
    return 10;
  }

  /**
   * Defines the time to keep the connection alive in a {@link okhttp3.ConnectionPool} before closing it
   *
   * @return keep alive time in milliseconds
   */
  @Value.Default
  default long keepAliveSeconds() {
    return 30;
  }

  /**
   * Applied when connecting a TCP socket to the target host. A value of 0 means no
   * timeout, otherwise values must be between 1 and {@link Integer#MAX_VALUE} when
   * converted to milliseconds. If unspecified, defaults to 10000.
   *
   * @return connect timeout duration in milliseconds
   */
  @Value.Default
  default long connectTimeoutMillis() {
    return 10000;
  }

  /**
   * Applied to both the TCP socket and for individual read IO operations. A value of
   * 0 means no timeout, otherwise values must be between 1 and {@link Integer#MAX_VALUE}
   * when converted to milliseconds. If unspecified, defaults to 30000.
   *
   * @return connect timeout duration in milliseconds
   */
  @Value.Default
  default long readTimeoutMillis() {
    return 30000;
  }

  /**
   * Applied to individual write IO operations. A value of 0 means no timeout,
   * otherwise values must be between 1 and {@link Integer#MAX_VALUE} when converted
   * to milliseconds. If unspecified, defaults to 30000.
   *
   * @return connect timeout duration in milliseconds
   */
  @Value.Default
  default long writeTimeoutMillis() {
    return 30000;
  }

  /**
   *
   * @return Maximum numbers of concurrent http requests (across all hosts).
   */
  @Value.Default
  default int maxRequests() {
    return 100;
  }

  /**
   *
   * @return Maximum numbers of concurrent http requests per host.
   */
  @Value.Default
  default int maxRequestsPerHost() {
   return 50;
  }
}
