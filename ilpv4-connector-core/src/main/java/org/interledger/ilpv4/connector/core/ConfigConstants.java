package org.interledger.ilpv4.connector.core;

/**
 * Constants related to this Connector's configuration.
 */
public interface ConfigConstants {

  String DOT = ".";
  String ENABLED = "enabled";
  String TRUE = "true";
  String FALSE = "false";

  String ILPV4__CONNECTOR = "ilpv4.connector";

  String ILPV4__CONNECTOR__INMEMORY_BALANCE_TRACKER__ENABLED =
    ILPV4__CONNECTOR + DOT + "inmem-balance-tracker" + DOT + "enabled";

}
