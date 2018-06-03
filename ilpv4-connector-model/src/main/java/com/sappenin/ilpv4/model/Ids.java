package com.sappenin.ilpv4.model;

import com.sappenin.ilpv4.model.immutables.Wrapped;
import com.sappenin.ilpv4.model.immutables.Wrapper;
import org.immutables.value.Value;

import java.util.UUID;

public interface Ids {

  /**
   * Identifier for {@link Account}.
   */
  @Value.Immutable
  @Wrapped
  abstract class _AccountId extends Wrapper<String> {

  }

  /**
   * Identifier for {@link Account}.
   */
  @Value.Immutable
  @Wrapped
  abstract class _PeerId extends Wrapper<UUID> {

  }
}