package org.interledger.ilpv4.connector.persistence.config;

import com.sappenin.interledger.ilpv4.connector.RuntimeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = RuntimeProperties.DB, havingValue = RuntimeProperties.Databases.H2)
public class H2ConnectorPersistenceConfig {


}
