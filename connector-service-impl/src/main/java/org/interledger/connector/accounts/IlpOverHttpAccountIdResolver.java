package org.interledger.connector.accounts;

import org.springframework.security.core.Authentication;

/**
 * Defines how to resolve AccountId for a given Ilp-over-Http connection.
 */
public interface IlpOverHttpAccountIdResolver extends AccountIdResolver {

  /**
   * Determine the {@link AccountId} for the supplied principal.
   *
   * @param authentication The {@link Authentication} to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied request.
   */
  AccountId resolveAccountId(Authentication authentication);
}
