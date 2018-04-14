package com.sappenin.ilpv4.model;

/**
 * A relationship type between two connectors. A connector will peer with other connectors, will be a child of a Teir-1
 * Connector, and a parent of a non-tier1 connector.
 */
public enum PeerType {
  PARENT,
  PEER,
  CHILD
}
