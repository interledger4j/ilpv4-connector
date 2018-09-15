package com.sappenin.ilpv4.connector.ccp;

import org.interledger.core.InterledgerAddressPrefix;
import org.immutables.value.Value;

/**
 * <p>Represents a withdrawn getRoute as advertised from a peer.</p>
 */
@Value.Immutable(intern = true)
public interface CcpWithdrawnRoute {

  /**
   * An Interledger address prefix that indicates the sender can no longer service this address prefix.
   */
  InterledgerAddressPrefix prefix();
}
