package org.interledger.connector.routing;

import org.interledger.connector.accounts.AccountId;

import org.immutables.value.Value;

/**
 * A container for holding information about the routing relationship that applies to a particular account (which is
 * simply a relationship between two nodes). When routing is enabled between two nodes, the nodes exchange routing
 * information.
 */
@Value.Immutable
public interface RoutableAccount {

  AccountId accountId();

  CcpSender ccpSender();

  CcpReceiver ccpReceiver();
}
