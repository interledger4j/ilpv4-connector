package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;

/**
 * <p>This manager provider higher-order logic surrouding accounts in a Connector. Generally, internal Connector
 * services will utilize an instance of {@link AccountSettingsRepository} directly for normal access to Account
 * Settings. However, this manager provides additional logic on top of the repository to enable logic that uses more
 * than the simple API provided by the Repository.</p>
 */
public interface AccountManager {

  /**
   * Accessor for the Account Settings repository that stores all account settings for this connector.
   *
   * @return The {@link AccountSettingsRepository}.
   */
  AccountSettingsRepository getAccountSettingsRepository();

  /**
   * Accessor for the Link Manager operated by this Connector.
   *
   * @return The {@link LinkManager}.
   */
  LinkManager getLinkManager();

  /**
   * Create a new account in this connector.
   *
   * @param accountSettings The {@link AccountSettings} for this account.
   *
   * @return The newly-created settings.
   */
  AccountSettings createAccount(AccountSettings accountSettings);

  /**
   * Helper method to initialize the parent account using IL-DCP. If this Connector is starting in `child` mode, it will
   * not have an operator address when it starts up, so this method finds the first account of type `parent` and uses
   * IL-DCP to get the operating address for this Connector.
   *
   * @param primaryParentAccountSettings The {@link AccountSettings} for the parent account to use IL-DCP with.
   *
   * @return The newly created parent {@link AccountSettings}.
   */
  AccountSettings initializeParentAccountSettingsViaIlDcp(AccountSettings primaryParentAccountSettings);

}
