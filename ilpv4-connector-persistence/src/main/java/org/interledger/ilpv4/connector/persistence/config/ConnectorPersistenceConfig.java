package org.interledger.ilpv4.connector.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The Connector's Spring Data configuration.
 */
@Configuration
@EnableJpaAuditing
@ComponentScan("org.interledger.ilpv4.connector.persistence")
@EnableJpaRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repositories")
@EntityScan("org.interledger.ilpv4.connector.persistence.entities")
@Import({
          // Placeholders for now (enable if we ever want custom processing that Spring Boot can't easily provide).
          //H2ConnectorPersistenceConfig.class,
          //PostgresqlConnectorPersistenceConfig.class
        })

public class ConnectorPersistenceConfig {
}
