package com.sappenin.interledger.ilpv4.connector.accounts;

import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;

/**
 * Defines how to operate on links outside the context of an {@link Account}.
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
  Link<?> createLink(final AccountId accountId, final LinkSettings linkSettings);

}
