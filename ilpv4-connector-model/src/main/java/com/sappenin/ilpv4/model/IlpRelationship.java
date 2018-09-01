package com.sappenin.ilpv4.model;

/**
 * <p>Defines the type of relationship between two ILP accounts, from the perspective of the Node operating the
 * account.</p>
 *
 * <p>Each account link will have one of three relationship-types that reflect how the account is related to the peer
 * on the other side of the link. These types include <tt>peer</tt>, <tt>parent</tt> or <tt>child</tt>.</p>
 *
 * <p>The Interledger network graph is organized in a tiered hierarchy, similar to the Internet, reflecting these
 * relationships. Large, high volume nodes are peered with one another to form the backbone of the network, using the
 * relationship type {@link IlpRelationship#PEER}. Smaller nodes will have links to these "tier 1" nodes and the link
 * will be of type {@link IlpRelationship#CHILD}, from the perspective of the tier 1 node; From the perspective of the
 * smaller node, the type will be {@link IlpRelationship#PARENT}.
 *
 * <p>A node MUST only have one link of type parent or, if it has multiple, only one configured to use the IL-DCP
 * protocol upon establishing the link, to request an address from the parent.</p>
 *
 * <p>A node that has links of type child must host an IL-DCP service to allow the nodes on those links to request
 * addresses. Generally these will be sub-addresses of the node's own address however this is not a requirement.</p>
 */
public enum IlpRelationship {
  /**
   * The ILP Node on the other side of this link is a parent of this node.
   */
  PARENT,

  /**
   * The ILP Node on the other side of this link is a peer of this node.
   */
  PEER,

  /**
   * The ILP Node on the other side of this link is a child of this node.
   */
  CHILD
}
