package org.interledger.connector.links;

import org.interledger.link.LinkSettings;

/**
 * Validator for {@link LinkSettings} instances. Expected to be called as part of creating account settings
 * to make sure that the account settings are not created with invalid or unusable settings.
 */
public interface LinkSettingsValidator {

  /**
   * Validates the provided link settings and optionally normalizes the values (for example, to use a consistent
   * shared secret encoded value).
   *
   * @param linkSettings settings to validate
   * @param <T> link type
   * @return validated and normalized settings
   */
  <T extends LinkSettings> T validateSettings(T linkSettings);

}
