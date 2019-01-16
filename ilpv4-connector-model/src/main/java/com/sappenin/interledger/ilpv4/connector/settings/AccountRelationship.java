package com.sappenin.interledger.ilpv4.connector.settings;

/**
 * <p>Defines the type of relationship between two ILP nodes for a given account, from the perspective of the Node
 * operating the link. For example, if a node is operating a link of type {@link AccountRelationship#CHILD}, then we
 * consider the remote link to be the child of the operating node (i.e., this link is my "child").</p>
 *
 * <p>Each link will have one of three relationship-types that reflect how the link is related to the peer
 * on the other side of the link. These types include <tt>peer</tt>, <tt>parent</tt> or <tt>child</tt>.</p>
 *
 * <p>The Interledger network topology is organized in a tiered hierarchy, similar to the Internet, reflecting these
 * relationships. Large, high volume nodes are peered with one another to form the backbone of the network, using the
 * relationship type {@link AccountRelationship#PEER}. Smaller nodes will have links to these "tier 1" nodes and the
 * link will be of type {@link AccountRelationship#CHILD}, from the perspective of the tier 1 node; From the
 * perspective of the smaller node, the type will be {@link AccountRelationship#PARENT}.
 *
 * <p>A node MUST only have one link of type parent or, if it has multiple, only one configured to use the IL-DCP
 * protocol upon establishing the link, to request an address from the parent.</p>
 *
 * <p>A node that has links of type child must host an IL-DCP service to allow the nodes on those links to request
 * addresses. Generally these will be sub-addresses of the node's own address however this is not a requirement.</p>
 */
public enum AccountRelationship {

  /**
   * Indicates that a link is for a node that is the `parent` of the node operating the link.
   */
  PARENT(0),

  /**
   * Indicates that a link is for a node that is the `peer` of the node operating the link.
   */
  PEER(1),

  /**
   * Indicates that a link is for a node that is the `child` of the node operating the link.
   */
  CHILD(2),

  /**
   * @deprecated May be removed if unused.
   */
  @Deprecated
  LOCAL(3);

  private int weight;

  AccountRelationship(final int weight) {
    this.weight = weight;
  }

  /**
   * The weight of this relationship.
   */
  public int getWeight() {
    return weight;
  }
}
