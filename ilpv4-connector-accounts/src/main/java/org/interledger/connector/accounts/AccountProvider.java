package org.interledger.connector.accounts;

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
