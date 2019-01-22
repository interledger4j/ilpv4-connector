package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import org.interledger.btp.BtpSession;
import org.interledger.plugin.lpiv2.Plugin;

/**
 * Defines how to resolve AccountId for a given {@link Plugin}.
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
