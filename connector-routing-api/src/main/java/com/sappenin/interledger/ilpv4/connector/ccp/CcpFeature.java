package com.sappenin.interledger.ilpv4.connector.ccp;

import org.immutables.value.Value;

/**
 * <p>An arbitrary feature for a {@link CcpNewRoute}.</p>
 *
 * <p>Currently, no features are in-use in the Network, but this value is here for future use.</p>
 *
 * <p>This value is interned because the values of this type contain only strings, which themselves are interned.
 * Additionally, the number of these features is expected to be finite and measurable performance improvements will be
 * gained by interning these instances due to not creating new immutable objects every time a new <tt>feature</tt> value
 * is encountered.</p>
 */
@Value.Immutable(intern = true)
public interface CcpFeature {
  String value();
}
