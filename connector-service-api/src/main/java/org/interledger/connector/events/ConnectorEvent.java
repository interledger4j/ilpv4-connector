package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountSettings;

import java.util.Optional;

/**
 * A parent interface for all Connector events.
 */
public interface ConnectorEvent {

  /**
   * The {@link AccountSettings} that triggered this event.
   */
  Optional<AccountSettings> accountSettings();

  /**
   * An arbitrary message that can be attached to this event.
   *
   * @return A {@link String}.
   */
  String message();

}
