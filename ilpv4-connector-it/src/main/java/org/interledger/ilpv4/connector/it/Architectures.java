package org.interledger.ilpv4.connector.it;

import org.interledger.ilpv4.connector.ConnectorServer;
import org.interledger.ilpv4.connector.it.graph.Graph;
import org.interledger.ilpv4.connector.it.graph.ServerVertex;
import org.interledger.ilpv4.connector.it.graph.edges.PeeringEdge;

/**
 * @author jfulton
 */
public class Architectures {

    static {
        System.setProperty("server.port","0");
        System.setProperty("spring.jmx.enabled", "false");
        System.setProperty("spring.application.admin.enabled","false");
    }

    public static final Graph simple() {
        return new Graph()
            .addVertex("g.david.usd", new ServerVertex(new ConnectorServer()))
            .addVertex("g.adrian.zar", new ServerVertex(new ConnectorServer()))
            .addVertex("g.jimmie.mxn", new ServerVertex(new ConnectorServer()))
            .addEdge(new PeeringEdge("g.david.usd", "g.adrian.zar"))
            .addEdge(new PeeringEdge("g.adrian.zar", "g.jimmie.mxn"))
            .addEdge(new PeeringEdge("g.jimmie.mxn", "g.david.usd"));
    }
}
