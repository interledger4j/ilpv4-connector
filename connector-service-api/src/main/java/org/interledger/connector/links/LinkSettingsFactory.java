package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.LinkSettings;

import java.util.Objects;

/**
 * A factory for constructing {@link LinkSettings} from various inputs.
 */
public interface LinkSettingsFactory {

  /**
   * Construct an instance of {@link LinkSettings} using the supplied {@code accountSettings}.
   *
   * @param accountSettings A {@link AccountSettings} used to construct link settings.
   *
   * @return A newly constructed instance of {@link LinkSettings} that corresponds to the supplied account settings.
   */
  LinkSettings construct(AccountSettings accountSettings);

  /**
   * Construct a more narrowly typed instance of {@link T} using the supplied {@code accountSettings}.
   *
   * @param accountSettings A {@link AccountSettings} used to construct link settings.
   *
   * @return A newly constructed instance of {@link LinkSettings} that corresponds to the supplied account settings.
   */
  default <T extends LinkSettings> T constructTyped(AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    return (T) this.construct(accountSettings);
  }
}
