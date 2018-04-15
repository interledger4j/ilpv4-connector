package org.interledger.ilpv4.connector.it.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfulton
 */
public class Graph {

    private Map<String, Vertex> vertices = new HashMap<>();
    private List<Edge> edges = new ArrayList<>();

    public Graph addVertex(String key, Vertex vertex) {
        vertices.put(key, vertex);
        return this;
    }

    public Graph addEdge(Edge edge) {
        edges.add(edge);
        return this;
    }

    public Vertex getVertex(String key) {
        return vertices.get(key);
    }

    public Graph start() {
        for (Vertex vertex : vertices.values()) {
            vertex.start();
        }
        for (Edge edge : edges) {
            edge.connect(this);
        }
        return this;
    }

    public void stop() {
        for (Vertex vertex : vertices.values()) {
            vertex.stop();
        }
    }
}
