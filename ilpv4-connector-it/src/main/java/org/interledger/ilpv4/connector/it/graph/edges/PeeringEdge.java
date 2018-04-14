package org.interledger.ilpv4.connector.it.graph.edges;

import org.interledger.ilpv4.connector.it.graph.Edge;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jfulton
 */
public class PeeringEdge extends Edge {

    private static final Logger logger = LoggerFactory.getLogger(PeeringEdge.class);
    private final String firstPeer;
    private final String secondPeer;

    public PeeringEdge(final String firstPeer, final String secondPeer) {
        this.firstPeer = firstPeer;
        this.secondPeer = secondPeer;
    }

    @Override
    public void connect(final Graph graph) {
        Vertex first = graph.getVertex(firstPeer);
        Vertex second = graph.getVertex(secondPeer);
        logger.info("Peering {} ({}:{}) and {} ({}:{} together",
            firstPeer, first.getHost(), first.getPort(),
            secondPeer, second.getHost(), second.getPort());
    }
}
