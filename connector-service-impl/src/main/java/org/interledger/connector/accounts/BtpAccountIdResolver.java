package org.interledger.connector.accounts;

import org.interledger.btp.BtpSession;
import org.interledger.link.Link;

/**
 * Defines how to resolve AccountId for a given {@link Link}.
 */
public interface BtpAccountIdResolver extends AccountIdResolver {

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param btpSession The {@link BtpSession} to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied plugin.
   */
  AccountId resolveAccountId(BtpSession btpSession);
}
