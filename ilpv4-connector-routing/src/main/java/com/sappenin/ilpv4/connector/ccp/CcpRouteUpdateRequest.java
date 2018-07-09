package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

import java.util.Collection;

@Value.Immutable
public interface CcpRouteUpdateRequest {

  String routingTableId();

  long currentEpochIndex();


  long fromEpochIndex();

  long toEpochIndex();

  long holdDownTime();

  String speaker();

  Collection<CcpRoute> newRoutes();

  Collection<InterledgerAddress> withdrawnRoutes();
}
