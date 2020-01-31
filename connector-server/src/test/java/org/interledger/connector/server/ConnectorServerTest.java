package org.interledger.connector.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectorServerTest {

  public static final String SPRING_PROFILES_ACTIVE = "--spring.profiles.active";
  public static final String SERVER_PORT = "--server.port";
  private ConnectorServer connectorServer;

  @Before
  public void setUp() {
    connectorServer = new ConnectorServer();
  }

  @After
  public void tearDown() {
    connectorServer.stop();
  }

  @Test
  public void migrateOnlyCallsStop() throws InterruptedException {
    System.setProperty(SPRING_PROFILES_ACTIVE, "migrate-only,test");
    System.setProperty(SERVER_PORT, "0");
    connectorServer.start();
    assertThat(connectorServer.getContext().isActive()).isFalse();
  }

  @Test
  public void serverActiveAfterStart() {
    System.setProperty(SPRING_PROFILES_ACTIVE, "test");
    System.setProperty(SERVER_PORT, "0");
    connectorServer.start();
    assertThat(connectorServer.getContext().isActive()).isTrue();
  }
}
