package org.interledger.connector.link;

import org.immutables.value.Value;
import org.interledger.support.immutables.Wrapped;
import org.interledger.support.immutables.Wrapper;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * A unique identifier for a {@link Link}. This value is generally only set once, (e.g., in a Connector to correlate a
   * {@link Link} to an account identifier) so that a link can be referenced across requests.
   *
   * @deprecated In the current model, a LinkId _is_ an AccountId (there is a one-to-one mapping) so this Id class will
   * go away in a future release.
   */
  @Value.Immutable
  @Wrapped
  @Deprecated
  static abstract class _LinkId extends Wrapper<String> {

  }

  /**
   * A wrapper that defines a "type" of link based upon a unique String.
   */
  @Value.Immutable
  @Wrapped
  static abstract class _LinkType extends Wrapper<String> {

    /**
     * Always normalize Link-type String values to full uppercase to avoid casing ambiguity in properties files.
     */
    @Value.Check
    public _LinkType normalize() {
      final String linkTypeString = this.value();
      if (!linkTypeString.toUpperCase().equals(linkTypeString)) {
        return LinkType.of(linkTypeString.toUpperCase());
      } else {
        return this;
      }
    }

  }
}
