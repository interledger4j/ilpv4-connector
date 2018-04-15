package org.interledger.ilpv4.connector.support;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

/**
 * @author jfulton
 */
public class ConnectorServerSettings {

    private ServletWebServerApplicationContext server;

    public ConnectorServerSettings(final ServletWebServerApplicationContext server) {
        this.server = server;
    }

    public int getPort() {
        return server.getWebServer().getPort();
    }

}
