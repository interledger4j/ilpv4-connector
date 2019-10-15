package org.interledger.connector.ccp;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddressPrefix;

/**
 * <p>Represents a withdrawn route as advertised from a peer.</p>
 */
@Value.Immutable(intern = true)
public interface CcpWithdrawnRoute {

  /**
   * An Interledger address prefix that indicates the sender can no longer service this address prefix.
   */
  InterledgerAddressPrefix prefix();
}
