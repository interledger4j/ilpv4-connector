package com.sappenin.interledger.ilpv4.connector.routing;

import org.immutables.value.Value;
import org.interledger.connector.accounts.Account;

/**
 * A container for holding information about the routing relationship that applies to a particular account (which is
 * simply a relationship between two nodes). When routing is enabled between two nodes, the nodes exchange routing
 * information.
 */
@Value.Immutable
public interface RoutableAccount {

  Account getAccount();

  CcpSender getCcpSender();

  CcpReceiver getCcpReceiver();
}
