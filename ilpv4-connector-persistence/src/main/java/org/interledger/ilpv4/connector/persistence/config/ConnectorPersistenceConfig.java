package org.interledger.ilpv4.connector.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The Connector's Spring Data configuration.
 */
@Configuration
@Import({
          H2ConnectorPersistenceConfig.class,
          RedisConnectorPersistenceConfig.class
        })
public class ConnectorPersistenceConfig {
}
