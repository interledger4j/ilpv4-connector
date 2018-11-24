package com.sappenin.interledger.ilpv4.connector.ccp;

import com.google.common.collect.ImmutableList;
import com.sappenin.interledger.ilpv4.connector.routing.RoutingTableId;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.UUID;

/**
 * A request sent from one ILP Node (the requestor) to another node (the counterpart) to control whether or not the
 * counterpart sends getRoute updates to the requestor. When the counterpart is in the {@link CcpSyncMode#MODE_IDLE},
 * then the counterpart does not send getRoute updates to the requestor node. Conversely,
 */
@Value.Immutable
public interface CcpRouteControlRequest {

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
   * The routing table ID that the requesting node knows about when this request is sent.
   *
   * @return A {@link UUID}.
   */
  RoutingTableId lastKnownRoutingTableId();

  /**
   * <p>The getEpoch that the requesting node knows about when this request is sent.</p>
   *
   * <p>Every time the routing table is modified, the update is logged and the revision number of the table is
   * increased. This revision number is called an <tt>getEpoch</tt>. A node maintains the getEpoch that is already known
   * to the counter node, and considers it when requesting getRoute updates so that the remote node can send only the
   * difference (a stack of new or withdrawn routes) when compared to the last routing update.</p>
   *
   * @return A <tt>long</tt> counter that
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
