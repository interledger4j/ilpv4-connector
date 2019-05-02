package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerProtocolException;

import java.util.Optional;
import java.util.Set;

/**
 * Defines how to operate on links.
 */
public interface LinkManager {

  /**
   * Creates an instance of {@link Link} using the supplied {@code accountId} and {@code linkSettings}.
   *
   * @param accountId    The {@link AccountId} of the link to create.
   * @param linkSettings The type of link to create.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * @throws RuntimeException if the link already exists.
   */
  default Link<?> createLink(AccountId accountId, LinkSettings linkSettings) {
    return this.createLink(accountId, linkSettings, false);
  }

  /**
   * Creates an instance of {@link Link} using the supplied {@code accountId} and {@code linkSettings}.
   *
   * @param accountId    The {@link AccountId} of the link to create.
   * @param linkSettings The type of link to create.
   * @param connect      A boolean that indicates if a connection should be attempted as part of creating this Link.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * @throws RuntimeException if the link already exists.
   */
  Link<?> createLink(AccountId accountId, LinkSettings linkSettings, boolean connect);

  /**
   * Accessor for the Link that a particular account is currently connected to this Connector via.
   *
   * @param accountId The {@link AccountId} of the links to retrieve.
   *
   * @return A {@link Link} that is currently connected for a particular accountId.
   */
  Optional<Link<?>> getConnectedLink(AccountId accountId);

  /**
   * Accessor for the Link that a particular account is currently connected to this Connector via, or a Reject exception
   * if the Link is not found.
   *
   * @param accountId The {@link AccountId} of the links to retrieve.
   *
   * @return A {@link Link} that is currently connected for a particular accountId.
   *
   * @throws InterledgerProtocolException If the Link is not found.
   */
  Link<?> getConnectedLinkSafe(AccountId accountId) throws InterledgerProtocolException;

  /**
   * Accessor for any links that are currently connected to this Connector.
   *
   * @return A {@link Link} that is currently connected for a particular accountId.
   */
  Set<Link<?>> getAllConnectedLinks();
}
