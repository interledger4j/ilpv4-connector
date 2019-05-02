package org.interledger.connector.accounts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.interledger.support.immutables.Wrapped;
import org.interledger.support.immutables.Wrapper;

import java.io.Serializable;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * A wrapper that defines a unique identifier for an account.
   */
  @Value.Immutable
  @Wrapped
  @JsonSerialize(as = AccountId.class)
  @JsonDeserialize(as = AccountId.class)
  static abstract class _AccountId extends Wrapper<String> implements Serializable {
    @Override
    public String toString() {
      return this.value();
    }
  }

  /**
   * A wrapper that defines a unique identifier for an account provider.
   */
  @Value.Immutable
  @Wrapped
  @JsonSerialize(as = AccountProviderId.class)
  @JsonDeserialize(as = AccountProviderId.class)
  static abstract class _AccountProviderId extends Wrapper<String> implements Serializable {
    @Override
    public String toString() {
      return this.value();
    }
  }

}