package com.sappenin.ilpv4.connector.routing;

import com.sappenin.ilpv4.InterledgerAddressPrefix;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.interledger.core.InterledgerAddress;

import java.time.Instant;
import java.util.List;
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

    //    /**
    //     * <p>Indicates whether some other object is "equal to" this one.</p>
    //     *
    //     * <p>Route equality is based upon the <tt>targetPrefix</tt> and <tt>nextHopAccount</tt> only so that in
    //     * general, there can exist multiple target prefixes pointing to different next-hop ledger accounts.</p>
    //     *
    //     * @param obj the reference object with which to compare.
    //     *
    //     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
    //     */
    //    @Override
    //    public boolean equals(Object obj) {
    //      if (this == obj) {
    //        return true;
    //      }
    //      if (obj == null || getClass() != obj.getClass()) {
    //        return false;
    //      }
    //
    //      RoutingTableEntry that = (RoutingTableEntry) obj;
    //
    //      if (!getTargetPrefix().equals(that.getTargetPrefix())) {
    //        return false;
    //      }
    //      if (!getNextHopAccount().equals(that.getNextHopAccount())) {
    //        return false;
    //      }
    //
    //      return true;
    //    }

    //    /**
    //     * <p>Overidden to satisfy to the equals-method contract, which  states that equal objects must have equal hash
    //     * codes.</p>
    //     *
    //     * @return a hash code value for this object.
    //     */
    //    @Override
    //    public int hashCode() {
    //      int result = getTargetPrefix().hashCode();
    //      result = 31 * result + getNextHopAccount().hashCode();
    //      return result;
    //    }

    //    @Value.Check
    //    void check() {
    //    }
  }
}
