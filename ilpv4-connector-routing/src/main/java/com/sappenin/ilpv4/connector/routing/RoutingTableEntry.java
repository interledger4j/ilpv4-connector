package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.interledger.core.InterledgerAddress;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * <p>An entry in a {@link RoutingTable}, used by Interledger nodes to determine the "next hop" account that a payment
 * should be forwarded to in order to complete a payment.</p>
 *
 * <p> For more details about the structure of this class as it relates to other routes in a routing table, reference
 * {@link RoutingTable}.</p>
 */
public interface RoutingTableEntry {

  String ALLOW_ALL_SOURCES = "(.*?)";

  /**
   * <p>An {@link InterledgerAddressPrefix} used to perform a longest-prefix match operation against a final
   * destination payment address. For example, if a payment is destined for <tt>g.example.alice</tt>, then the
   * longest-matching target prefix might be <tt>g.example</tt>, assuming such an entry exists in the routing
   * table.</p>
   *
   * <p>This value is typed as a {@link String} as opposed to an <tt>InterledgerAddress</tt> so that it might
   * contain non-valid address characters, such as a Regex.</p>
   *
   * @return A {@link String}.
   */
  InterledgerAddressPrefix getTargetPrefix();

  /**
   * <p>An {@link InterledgerAddress} representing the account that should be listed as the recipient of any next-hop
   * ledger transfers.</p>
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getNextHopAccount();

  /**
   * A list of nodes that a payment will traverse in order for a payment to make it to its destination.
   *
   * @return
   */
  List<InterledgerAddress> getPath();

  /**
   * <p>A regular expression that can restrict routing table destinations to a subset of allowed payment-source
   * prefixes. By default, this filter allows all sources.</p>
   *
   * @return A {@link Pattern}
   */
  default Pattern getSourcePrefixRestrictionRegex() {
    // Default to allow all sources.
    return Pattern.compile(ALLOW_ALL_SOURCES);
  }

  /**
   * <p>An optionally-present expiration date/time for this route.</p>
   *
   * @return An {@link Instant} representing the
   */
  Optional<Instant> getExpiresAt();

  /**
   * Bytes that can be used for authentication of a given route.
   *
   * @return
   */
  default byte[] getAuth() {
    return new byte[0];
  }

  /**
   * An abstract implementation of {@link RoutingTableEntry} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractRoutingTableEntry implements RoutingTableEntry {

    @Default
    @Override
    public Pattern getSourcePrefixRestrictionRegex() {
      // Default to allow all sources.
      return Pattern.compile(ALLOW_ALL_SOURCES);
    }

    /**
     * Bytes that can be used for authentication of a given route.
     *
     * @return
     */
    @Default
    public byte[] getAuth() {
      return new byte[0];
    }

    // These are overridden because Pattern does not define an equals method, which default to Object equality, making
    // no pattern ever equal to any other pattern.

    /**
     * This instance is equal to all instances of {@code ImmutableRoutingTableEntry} that have equal attribute values.
     *
     * @return {@code true} if {@code this} is equal to {@code another} instance
     */
    @Override
    public boolean equals(Object another) {
      if (this == another) {
        return true;
      }
      return another instanceof ImmutableRoutingTableEntry
        && equalTo((ImmutableRoutingTableEntry) another);
    }

    private boolean equalTo(ImmutableRoutingTableEntry another) {
      return getSourcePrefixRestrictionRegex().pattern().equals(another.getSourcePrefixRestrictionRegex().pattern())
        && Arrays.equals(getAuth(), another.getAuth())
        && getTargetPrefix().equals(another.getTargetPrefix())
        && getNextHopAccount().equals(another.getNextHopAccount())
        && getPath().equals(another.getPath())
        && Objects.equals(getExpiresAt(), another.getExpiresAt());
    }

    /**
     * Computes a hash code from attributes: {@code sourcePrefixRestrictionRegex}, {@code auth}, {@code targetPrefix},
     * {@code nextHopAccount}, {@code path}, {@code expiresAt}.
     *
     * @return hashCode value
     */
    @Override
    public int hashCode() {
      int h = 5381;
      h += (h << 5) + getSourcePrefixRestrictionRegex().pattern().hashCode();
      h += (h << 5) + Arrays.hashCode(getAuth());
      h += (h << 5) + getTargetPrefix().hashCode();
      h += (h << 5) + getNextHopAccount().hashCode();
      h += (h << 5) + getPath().hashCode();
      h += (h << 5) + Objects.hashCode(getExpiresAt());
      return h;
    }

  }
}
