package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.Account;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;

/**
 * Provides new accounts for the connector to track.
 */
public interface AccountProvider {

  /**
   * Construct a new Account based on the supplied account-settings.
   *
   * @param defaultAccountSettings An {@link AccountSettings} representing the settings this account should have.
   */
  Account constructNewAccount(AccountSettings defaultAccountSettings);

}
