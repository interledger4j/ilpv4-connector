package org.interledger.connector.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ConnectorServerTest {

  @Test
  public void migrateOnlyCallsStop() {
    System.setProperty("--spring.profiles.active", "test,migrate-only");
    System.setProperty("--server.port", "0");
    System.setProperty("--spring.liquibase.enabled", "false");
    ConnectorServer connectorServer = new ConnectorServer();
    connectorServer.start();
    assertThat(connectorServer.getContext().isActive()).isFalse();
  }
}
