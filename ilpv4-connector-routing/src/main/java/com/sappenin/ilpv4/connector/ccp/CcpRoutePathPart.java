package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

/**
 * <p>An individual component of an overall path in a {@link CcpNewRoute}. For example, if a path traverses 3 nodes, then
 * each node in that path would be considered a {@link CcpRoutePathPart}.</p>
 *
 * <p>This value is interned because the values of this type contain only interned values. Additionally, the number of
 * these objects is expected to be finite and measurable performance improvements will be gained by interning these
 * instances due to not creating new immutable objects every time a new instance is required.</p>
 */
@Value.Immutable(intern = true)
public interface CcpRoutePathPart {
  InterledgerAddress routePathPart();
}
