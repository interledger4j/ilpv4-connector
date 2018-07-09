package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Collection;
import java.util.List;

@Value.Immutable
public interface CcpRoute {

  InterledgerAddress prefix();

  List<InterledgerAddress> path();

  String auth();

  Collection<CcpRouteProperties> properties();
}
