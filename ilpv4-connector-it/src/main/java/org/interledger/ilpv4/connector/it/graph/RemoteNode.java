package org.interledger.ilpv4.connector.it.graph;

/**
 * @author jfulton
 */
public class RemoteNode implements Node {

    private final String host;
    private final int port;

    public RemoteNode(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
