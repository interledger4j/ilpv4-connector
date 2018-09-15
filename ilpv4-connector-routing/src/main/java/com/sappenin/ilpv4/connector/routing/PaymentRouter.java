package com.sappenin.ilpv4.connector.routing;

import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerAddress;

import java.util.Optional;

/**
 * An interface that determines a "best" next-hop for a particular Interledger payment based upon implementation-defined
 * metrics. For example, while a given routing table might contain multiple valid routes for a payment to traverse, one
 * implementation of this interface might decide to always choose the "closest" getRoute, whereas another implementation
 * might decide to use the "most reliable" getRoute.
 */
public interface PaymentRouter<R extends BaseRoute> {

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" getRoute that should
   * be utilized to complete an Interledger payment.
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

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" getRoute that should
   * be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment pathParts from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   * @param sourceLedgerPrefix      An {@link InterledgerAddressPrefix} prefix that indicates the ILP node that received
   *                                the payment being routed. This value is used to optionally restrict the set of
   *                                available routes that can be used to service a next-hop payment or message.
   */
  //Optional<R> findBestNexHop(InterledgerAddress finalDestinationAddress, InterledgerAddressPrefix sourceLedgerPrefix);
}
