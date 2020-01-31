package org.interledger.connector.server.spring.settings.properties;

import org.interledger.connector.settings.IlpOverHttpConnectionSettings;

public class IlpOverHttpConnectionSettingsFromPropertyFile implements IlpOverHttpConnectionSettings {

  private long connectTimeoutMillis;
  private long readTimeoutMillis;
  private long writeTimeoutMillis;
  private int maxRequests;
  private int maxRequestsPerHost;
  private int maxIdleConnections;
  private long keepAliveSeconds;

  @Override
  public long connectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  @Override
  public long readTimeoutMillis() {
    return readTimeoutMillis;
  }

  @Override
  public long writeTimeoutMillis() {
    return writeTimeoutMillis;
  }

  @Override
  public int maxRequests() {
    return maxRequests;
  }

  @Override
  public int maxRequestsPerHost() {
    return maxRequestsPerHost;
  }

  @Override
  public int maxIdleConnections() {
    return maxIdleConnections;
  }

  @Override
  public long keepAliveSeconds() {
    return keepAliveSeconds;
  }

  public void setConnectTimeoutMillis(long connectTimeoutMillis) {
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  public void setReadTimeoutMillis(long readTimeoutMillis) {
    this.readTimeoutMillis = readTimeoutMillis;
  }

  public void setWriteTimeoutMillis(long writeTimeoutMillis) {
    this.writeTimeoutMillis = writeTimeoutMillis;
  }

  public void setMaxRequests(int maxRequests) {
    this.maxRequests = maxRequests;
  }

  public void setMaxRequestsPerHost(int maxRequestsPerHost) {
    this.maxRequestsPerHost = maxRequestsPerHost;
  }

  public void setMaxIdleConnections(int maxIdleConnections) {
    this.maxIdleConnections = maxIdleConnections;
  }

  public void setKeepAliveSeconds(long keepAliveSeconds) {
    this.keepAliveSeconds = keepAliveSeconds;
  }
}
