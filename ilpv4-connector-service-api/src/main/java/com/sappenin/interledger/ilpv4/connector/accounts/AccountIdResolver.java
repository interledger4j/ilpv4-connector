package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.plugin.lpiv2.Plugin;

/**
 * Defines how to resolve AccountId for a given {@link Plugin}.
 */
public interface AccountIdResolver {

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param plugin The plugin to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied plugin.
   */
  AccountId resolveAccountId(Plugin<?> plugin);

}
