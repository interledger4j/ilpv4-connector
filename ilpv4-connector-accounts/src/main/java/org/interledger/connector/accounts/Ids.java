package org.interledger.connector.accounts;

import org.immutables.value.Value;
import org.interledger.support.immutables.Wrapped;
import org.interledger.support.immutables.Wrapper;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * A wrapper that defines a unique identifier for an account.
   */
  @Value.Immutable
  @Wrapped
  static abstract class _AccountId extends Wrapper<String> {

  }

  /**
   * A wrapper that defines a unique identifier for an account provider.
   */
  @Value.Immutable
  @Wrapped
  static abstract class _AccountProviderId extends Wrapper<String> {

  }

}