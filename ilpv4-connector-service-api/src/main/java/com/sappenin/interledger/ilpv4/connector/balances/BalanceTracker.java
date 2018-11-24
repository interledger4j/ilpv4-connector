package com.sappenin.interledger.ilpv4.connector.balances;

/**
 * A service for tracking balances between accounts/plugins in an ILPv4 Connector.
 */
public interface BalanceTracker {

  void onIncomingData();

  void onOutgoingData();




}
