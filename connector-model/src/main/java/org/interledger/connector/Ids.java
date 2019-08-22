package org.interledger.connector;

import org.immutables.value.Value;
import org.interledger.support.immutables.Wrapped;
import org.interledger.support.immutables.Wrapper;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * A wrapper that defines a "type" of bilateral based upon a unique String. For example, "gRPC" or "WebSockets".
   */
  @Value.Immutable
  @Wrapped
  static abstract class _BilateralConnectionType extends Wrapper<String> {

  }

}
