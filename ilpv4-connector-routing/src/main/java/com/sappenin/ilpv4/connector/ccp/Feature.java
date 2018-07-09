package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;

// TODO: Make this interned.
@Value.Immutable
public interface Feature {
  String value();
}
