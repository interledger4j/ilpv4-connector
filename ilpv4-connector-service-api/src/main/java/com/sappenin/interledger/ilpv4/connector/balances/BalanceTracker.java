package com.sappenin.interledger.ilpv4.connector.balances;

/**
 * A service for tracking balances between accounts/links in an ILPv4 Connector.
 */
public interface BalanceTracker {

  void onIncomingData();

  void onOutgoingData();




}
