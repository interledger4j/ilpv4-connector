package org.interledger.ilpv4.connector.config;

import com.sappenin.ilpv4.server.ConnectorConfiguration;
import org.interledger.ilpv4.connector.support.ConnectorServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author jfulton
 */
@SpringBootApplication
//@Import(ConnectorConfiguration.class)
public class ConnectorServerConfig {

    @Autowired
    private ServletWebServerApplicationContext server;

    @Bean
    public ConnectorServerSettings settings() {
        return new ConnectorServerSettings(server);
    }
}
