package com.sappenin.ilpv4.connector.ccp;

import org.immutables.value.Value;
import org.interledger.core.InterledgerAddress;

/**
 * <p>Represents a withdrawn route as advertised from a peer.</p>
 *
 * <p>This value is <tt>not</tt> interned because the number of times this object will be encountered is expected to
 * be somewhat low, and interning would likely create unnecessary memory overhead.</p>
 */
@Value.Immutable
public interface CcpWithdrawnRoute {
  InterledgerAddress prefix();
}
