package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;

import java.util.Set;

/**
 * Defines how to operate on links.
 */
public interface LinkManager {

  /**
   * Gets or creates the Link that the account is currently connected to this Connector via.
   *
   * @param accountId The {@link AccountId} of the links to retrieve.
   *
   * @return A {@link Link} that is currently connected for a particular accountId.
   */
  Link<? extends LinkSettings> getOrCreateLink(AccountId accountId);

  /**
   * Gets or creates the Link that the account is currently connected to this Connector via.
   *
   * @param accountSettings The {@link AccountSettings} corresponding to the link to create.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * // TODO: Create typed problem here...
   *
   * @throws RuntimeException if the link already exists.
   */
  Link<? extends LinkSettings> getOrCreateLink(AccountSettings accountSettings);

  /**
   * Gets or creates the Link that the account is currently connected to this Connector via.
   *
   * @param accountId    The {@link AccountId} of the link to create.
   * @param linkSettings The type of link to create.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * @throws RuntimeException if the link already exists.
   */
  Link<? extends LinkSettings> getOrCreateLink(AccountId accountId, LinkSettings linkSettings);

  /**
   * Accessor for any links that are currently connected to this Connector.
   *
   * @return A {@link Link} that is currently connected for a particular accountId.
   */
  Set<Link<?>> getAllConnectedLinks();
}
