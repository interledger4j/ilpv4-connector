package org.interledger.connector.accounts;

import org.interledger.link.Link;

/**
 * Defines how to resolve Account settings for a given {@link AccountId}.
 */
public interface AccountSettingsResolver {

  /**
   * Determine the {@link AccountId} for the supplied link.
   *
   * @param link The Plugin to resolve Account Settings for.
   *
   * @return An instance of {@link AccountSettings}.
   */
  AccountSettings resolveAccountSettings(Link<?> link);

}
