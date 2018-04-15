package org.interledger.ilpv4.connector.it.graph;

/**
 * @author jfulton
 */
public interface Vertex {

    String getHost();

    int getPort();

    void start();

    void stop();
}
