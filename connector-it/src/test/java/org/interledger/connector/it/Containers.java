package org.interledger.connector.it;

import org.slf4j.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public final class Containers {

  public static GenericContainer redis(Network network) {
    return new GenericContainer("redis:5.0.6")
        .withNetwork(network)
        .withNetworkAliases("redis")
        .withEnv("REDIS_URL", "redis://redis:6379");
  }

  public static GenericContainer postgres(Network network, String databaseName, String schemaResource) {
    return new FixedHostPortGenericContainer("postgres:12")
        .withFixedExposedPort(35432, 5432)
        .withEnv("POSTGRES_USER", databaseName)
        .withEnv("POSTGRES_PASSWORD", databaseName)
        .withEnv("POSTGRES_DB", databaseName)
        .withClasspathResourceMapping(
            schemaResource,
            "/docker-entrypoint-initdb.d/1-init.sql",
            BindMode.READ_ONLY
        )
        .withNetwork(network);
  }

  public static GenericContainer settlement(Network network, int port, int connectorPort) {
    return settlement(network, port, connectorPort, null);
  }

  public static GenericContainer settlement(Network network, int port, int connectorPort, Logger logger) {
    GenericContainer container = new GenericContainer<>("interledgerjs/settlement-xrp")
        .withExposedPorts(port)
        .withCreateContainerCmdModifier(e -> e.withPortSpecs())
        .withNetwork(network)
        .withEnv("REDIS_URI", "redis://redis:6379")
        .withEnv("ENGINE_PORT", String.valueOf(port))
        .withEnv("DEBUG", "settlement*")
        .withEnv("CONNECTOR_URL", "http://host.docker.internal:" + connectorPort);
    if (logger != null) {
      container = container.withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger));
    }
    return container;
  }
}
