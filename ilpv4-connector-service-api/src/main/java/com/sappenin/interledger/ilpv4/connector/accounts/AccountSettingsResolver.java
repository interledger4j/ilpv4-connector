package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import org.interledger.plugin.lpiv2.Plugin;

/**
 * Defines how to resolve Account settings for a given {@link AccountId}.
 */
public interface AccountSettingsResolver {

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param plugin The Plugin to resolve Account Settings for.
   *
   * @return An instance of {@link AccountSettings}.
   */
  AccountSettings resolveAccountSettings(Plugin<?> plugin);

}
