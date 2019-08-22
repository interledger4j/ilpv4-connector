package com.sappenin.interledger.ilpv4.connector;

import org.immutables.value.Value;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

/**
 * A statically configured route. Static routes take precedence over the same or shorter prefixes that are local or
 * published by peers. More specific prefixes will still take precedence.
 */
public interface StaticRoute {

  InterledgerAddressPrefix SELF_INTERNAL = InterledgerAddressPrefix.SELF.with("internal");
  InterledgerAddress STANDARD_DEFAULT_ROUTE = InterledgerAddress.of(SELF_INTERNAL.getValue());

  static ImmutableStaticRoute.Builder builder() {
    return ImmutableStaticRoute.builder();
  }

  /**
   * A target address prefix that corresponds to {@code #getPeerAddress}.
   *
   * @return
   */
  InterledgerAddressPrefix getTargetPrefix();

  /**
   * The ILP address of the peer this route should route through.
   *
   * @return
   */
  AccountId getPeerAccountId();

  @Value.Immutable(intern = true)
  abstract class AbstractStaticRoute implements StaticRoute {

  }

}
