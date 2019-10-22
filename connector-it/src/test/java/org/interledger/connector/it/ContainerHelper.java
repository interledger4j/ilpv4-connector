package org.interledger.connector.it;

import org.slf4j.Logger;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public final class ContainerHelper {

  public static GenericContainer redis(Network network) {
    return new FixedHostPortGenericContainer("redis:5.0.6")
        .withFixedExposedPort(36379, 6379)
        .withNetwork(network)
        .withNetworkAliases("redis")
        .withEnv("REDIS_URL", "redis://redis:36379");
  }

  public static GenericContainer postgres(Network network) {
    return new FixedHostPortGenericContainer("postgres:12")
        .withFixedExposedPort(35432, 5432)
        .withEnv("POSTGRES_USER", "connector")
        .withEnv("POSTGRES_PASSWORD", "connector")
        .withEnv("POSTGRES_DB", "connector")
        .withClasspathResourceMapping(
            "initialization.sql",
            "/docker-entrypoint-initdb.d/1-init.sql",
            BindMode.READ_ONLY
        )
        // this gets emitted twice before the db is ready: once before the init scripts run and once after
        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2))
        .withNetwork(network);
  }

  public static GenericContainer settlement(Network network, int port, int connectorPort) {
    return settlement(network, port, connectorPort, null);
  }

  public static GenericContainer settlement(Network network, int port, int connectorPort, Logger logger) {
    Testcontainers.exposeHostPorts(connectorPort);
    GenericContainer container = new GenericContainer<>("interledgerjs/settlement-xrp")
        .withExposedPorts(port)
        .withCreateContainerCmdModifier(e -> e.withPortSpecs())
        .withNetwork(network)
        .withEnv("REDIS_URI", "redis://redis:6379")
        .withEnv("ENGINE_PORT", String.valueOf(port))
        .withEnv("DEBUG", "settlement*")
        .withEnv("CONNECTOR_URL", "http://host.testcontainers.internal:" + connectorPort);
    if (logger != null) {
      container = container.withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger));
    }
    return container;
  }
}
