package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;

import java.security.Principal;

/**
 * Defines how to resolve AccountId for a given BLAST connection.
 */
public interface BlastAccountIdResolver extends AccountIdResolver {

  /**
   * Determine the {@link AccountId} for the supplied principal.
   *
   * @param principal The {@link Principal} to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied request.
   */
  AccountId resolveAccountId(Principal principal);
}
