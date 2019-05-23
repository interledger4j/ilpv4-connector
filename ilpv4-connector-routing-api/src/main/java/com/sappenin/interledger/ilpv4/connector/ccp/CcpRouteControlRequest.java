package com.sappenin.interledger.ilpv4.connector.ccp;

import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingTableId;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Optional;

/**
 * A request sent from one ILP Node (the requestor) to another node (the counterpart) to control whether or not the
 * counterpart sends getRoute updates to the requestor. When the counterpart is in the {@link CcpSyncMode#MODE_IDLE},
 * then the counterpart does not send getRoute updates to the requestor node. Conversely,
 */
@Value.Immutable
public interface CcpRouteControlRequest {

  static ImmutableCcpRouteControlRequest.Builder builder() {
    return ImmutableCcpRouteControlRequest.builder();
  }

  /**
   * The state that the node wants the counterpart node to be in.
   *
   * @return A {@link CcpSyncMode}.
   */
  @Value.Default
  default CcpSyncMode getMode() {
    return CcpSyncMode.MODE_IDLE;
  }

  /**
   * The routing table ID that the requesting node knows about when this request is sent. This may be {@link
   * Optional#empty()}, such as when no routing-table identifier has yet been received from a remote peer.
   *
   * @return A {@link RoutingTableId}.
   */
  Optional<RoutingTableId> lastKnownRoutingTableId();

  /**
   * <p>The epoch that the requesting node knows about when this request is sent.</p>
   *
   * <p>Every time the routing table is modified, the update is logged and the revision number of the table is
   * increased. This revision number is called an <tt>epoch</tt>. A node maintains the epoch that is already known to
   * the corresponding node, and considers it when requesting route updates so that the remote node can send only the
   * difference (a stack of new or withdrawn routes) when compared to the last routing update.</p>
   *
   * @return A <tt>long</tt> counter that represents the epoch. NOTE: this value is serialized as an unsigned 32-bit
   * number in OER, which technically supports something larger than an `int`. However, the  it's possible that the
   * actual epoch might exceed the on-the-wire encoding. However, in order to do this, the Connector would need to
   * process more than 2^31 (~2.1 billion) route updates, which is unlikely to happen since Connectors generally only
   * send updates every 30 seconds, which means it would take about 65 years without a restart for an overflow to occur
   * (so this will never happen).
   */
  int lastKnownEpoch();

  /**
   * @return
   */
  @Value.Default
  default Collection<CcpFeature> features() {
    return ImmutableList.of();
  }
}
