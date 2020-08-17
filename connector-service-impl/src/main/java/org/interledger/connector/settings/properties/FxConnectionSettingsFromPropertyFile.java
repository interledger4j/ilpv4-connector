package org.interledger.connector.settings.properties;

import org.interledger.connector.settings.FxConnectionSettings;

public class FxConnectionSettingsFromPropertyFile implements FxConnectionSettings {

  private int maxIdleConnections = 5;

  private long keepAliveMinutes = 1;

  private long connectTimeoutMillis = 1000;

  private long readTimeoutMillis = 30000;

  private long writeTimeoutMillis = 30000;

  @Override
  public int maxIdleConnections() {
    return maxIdleConnections;
  }

  @Override
  public long keepAliveMinutes() {
    return keepAliveMinutes;
  }

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


  public void setMaxIdleConnections(int maxIdleConnections) {
    this.maxIdleConnections = maxIdleConnections;
  }

  public void setKeepAliveMinutes(long keepAliveMinutes) {
    this.keepAliveMinutes = keepAliveMinutes;
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
}
