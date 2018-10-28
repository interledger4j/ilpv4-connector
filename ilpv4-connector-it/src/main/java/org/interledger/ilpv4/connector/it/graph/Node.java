package org.interledger.ilpv4.connector.it.graph;

import okhttp3.HttpUrl;

/**
 * A node in a graph.
 */
public interface Node {

  /**
   * The Http scheme this node uses.
   */
  String getScheme();

  /**
   * The hostname for this node (can be used to create an HttpUrl).
   */
  String getHost();

  /**
   * The port this node is running on.
   */
  int getPort();

  /**
   * Get this node's endpoint location as an HTTP URL.
   */
  default HttpUrl getNodeUrl() {
    return HttpUrl.parse(getHost()).newBuilder().port(getPort()).build();
  }

  void start();

  void stop();
}
