package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;

/**
 * Defines how to resolve Account settings for a given {@link AccountId}.
 */
public interface AccountSettingsResolver {

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param accountId The unique identifier for the account to resolve account-settings for.
   *
   * @return An instance of {@link AccountSettings}.
   */
  AccountSettings resolveAccountSettings(AccountId accountId);

}
