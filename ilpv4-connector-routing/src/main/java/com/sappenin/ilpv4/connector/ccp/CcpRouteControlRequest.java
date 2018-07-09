package com.sappenin.ilpv4.connector.ccp;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.UUID;

import static com.sappenin.ilpv4.connector.ccp.CcpMode.MODE_IDLE;

@Value.Immutable
public interface CcpRouteControlRequest {

  @Value.Default
  default CcpMode getMode() {
    return MODE_IDLE;
  }

  UUID lastKnownRoutingTableId();

  long lastKnownEpoch();

  @Value.Default
  default Collection<Feature> features() {
    return ImmutableList.of();
  }
}
