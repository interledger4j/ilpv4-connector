package org.interledger.ilpv4.connector;

import org.interledger.ilpv4.connector.config.ConnectorServerConfig;
import org.interledger.ilpv4.connector.support.Server;

/**
 * @author jfulton
 */
public class ConnectorServer extends Server {

    public ConnectorServer() {
        super(ConnectorServerConfig.class);
    }
}
