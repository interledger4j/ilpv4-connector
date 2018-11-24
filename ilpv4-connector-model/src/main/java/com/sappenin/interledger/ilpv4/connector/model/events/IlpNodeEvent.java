package com.sappenin.interledger.ilpv4.connector.model.events;

import org.immutables.value.Value;

/**
 * The parent interface for all Connector events.
 */
@Value
public interface IlpNodeEvent<T> {

  String getMessage();

  T getObject();
}