package org.interledger.ilpv4.connector.it.graph;

import org.interledger.ilpv4.connector.support.Server;

/**
 * @author jfulton
 */
public class ServerVertex implements Vertex {

    private final Server server;

    public ServerVertex(final Server server) {
        this.server = server;
    }

    @Override
    public String getHost() {
        return "localhost";
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop();
    }
}
