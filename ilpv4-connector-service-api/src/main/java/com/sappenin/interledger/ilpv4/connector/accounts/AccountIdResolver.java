package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;

/**
 * Defines how to resolve AccountId for a given {@link Link}.
 */
public interface AccountIdResolver {

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param link The {@link Link} to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied plugin.
   */
  AccountId resolveAccountId(Link<?> link);
}
