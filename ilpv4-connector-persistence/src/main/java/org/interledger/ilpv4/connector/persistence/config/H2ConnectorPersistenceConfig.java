package org.interledger.ilpv4.connector.persistence.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.jpa.database", havingValue = "h2")
public class H2ConnectorPersistenceConfig {
}
