package com.sappenin.interledger.ilpv4.connector.events;

/**
 * The parent interface for all Connector events.
 */
public interface IlpNodeEvent<T> {

  String getMessage();

  T getObject();
}