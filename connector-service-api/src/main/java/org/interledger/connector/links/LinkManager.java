package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.link.exceptions.LinkNotConnectedException;

import java.util.Set;

/**
 * Defines how to operate on links.
 */
public interface LinkManager {

  /**
   * Gets or creates the Link that the account is currently connected to this Connector via.
   *
   * @param accountId The {@link AccountId} corresponding to the link to create.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * @throws LinkNotConnectedException if the link cannot be created (e.g., a Websocket client link).
   */
  Link<? extends LinkSettings> getOrCreateLink(AccountId accountId) throws LinkNotConnectedException;

  /**
   * Gets or creates the Link that the account is currently connected to this Connector via.
   *
   * @param accountSettings The {@link AccountSettings} corresponding to the link to create.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * @throws LinkNotConnectedException if the link cannot be created (e.g., a Websocket client link).
   */
  Link<? extends LinkSettings> getOrCreateLink(AccountSettings accountSettings) throws LinkNotConnectedException;

  /**
   * Gets or creates the Link that the account is currently connected to this Connector via.
   *
   * @param accountId    The {@link AccountId} of the link to create.
   * @param linkSettings The type of link to create.
   *
   * @return A newly constructed instance of {@link Link}.
   *
   * @throws LinkNotConnectedException if the link cannot be created (e.g., a Websocket client link).
   */
  Link<? extends LinkSettings> getOrCreateLink(AccountId accountId, LinkSettings linkSettings)
    throws LinkNotConnectedException;

  /**
   * Accessor for any links that are currently connected to this Connector.
   *
   * @return A {@link Link} that is currently connected for a particular accountId.
   */
  Set<Link<? extends LinkSettings>> getAllConnectedLinks();

  /**
   * Accessor for the {@link Link} that processes SPSP-compatible STREAM packets. Each account will utilize a
   * potentially different asset code and scale, so this interface allows for varying instances of an SPSP Link on a
   * per-account basis.
   *
   * @return A {@link Link} for processing STREAM packets.
   */
  Link<? extends LinkSettings> getOrCreateSpspReceiverLink(final AccountSettings accountSettings);

}
