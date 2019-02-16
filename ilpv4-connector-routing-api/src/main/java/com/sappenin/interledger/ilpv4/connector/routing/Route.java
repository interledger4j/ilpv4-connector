package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.hash.Hashing;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <p>An entry in a {@link RoutingTable}, used by Interledger nodes to determine the "next hop" account that a payment
 * should be forwarded to in order to complete a payment.</p>
 *
 * <p> For more details about the structure of this class as it relates to other routes in a routing table, reference
 * {@link RoutingTable}.</p>
 */
public interface Route extends BaseRoute {

  byte[] EMPTY_AUTH = new byte[32];

  static ImmutableRoute.Builder builder() {
    return ImmutableRoute.builder();
  }

  /**
   * Create an HMAC of the routing secret and address prefix using Hmac SHA256.
   */
  static byte[] HMAC(String routingSecret, InterledgerAddressPrefix addressPrefix) {
    return Hashing
      .hmacSha256(routingSecret.getBytes(UTF_8))
      .hashBytes(addressPrefix.getValue().getBytes(UTF_8)).asBytes();
  }

  /**
   * <p>An {@link InterledgerAddress} representing the account that should be listed as the recipient of any next-hop
   * ledger transfers for this route.</p>
   *
   * @return An {@link InterledgerAddress}.
   */
  AccountId getNextHopAccountId();

  /**
   * A list of nodes that a payment will traverse in order for a payment to make it to its destination.
   *
   * @return
   */
  List<InterledgerAddress> getPath();

  /**
   * <p>An optionally-present expiration date/time for this getRoute.</p>
   *
   * @return An {@link Instant} representing the
   */
  Optional<Instant> getExpiresAt();

  /**
   * Bytes that can be used for authentication of a given getRoute. Reserved for the future, currently not used.
   *
   * @return
   */
  @Default
  default byte[] getAuth() {
    return EMPTY_AUTH;
  }

  /**
   * An abstract implementation of {@link Route} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractRoute implements Route {

  }
}
