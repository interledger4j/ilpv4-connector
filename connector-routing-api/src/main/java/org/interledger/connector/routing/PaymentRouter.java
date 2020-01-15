package org.interledger.connector.routing;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import java.util.Optional;

/**
 * An interface that determines a "best" next-hop for a particular Interledger payment based upon implementation-defined
 * metrics. For example, while a given routing table might contain multiple valid routes for a payment to traverse, one
 * implementation of this interface might decide to always choose the "closest" route, whereas another implementation
 * might decide to use the "most reliable" route.
 */
public interface PaymentRouter<R extends BaseRoute> {

  // The unique identifier of the account that collects all incoming ping protocol payments, if any.
  // TODO: Move to connector-core as part of https://github.com/interledger4j/ilpv4-connector/issues/148
  AccountId PING_ACCOUNT_ID = AccountId.of("__ping_account__");

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" route that
   * should be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment pathParts from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   */
  Optional<R> findBestNexHop(InterledgerAddress finalDestinationAddress);

}
