package org.interledger.connector.accounts;

import org.interledger.btp.BtpSession;
import org.interledger.btp.BtpSessionCredentials;
import org.interledger.link.Link;
import org.interledger.link.StatefulLink;
import org.interledger.link.exceptions.LinkNotConnectedException;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.security.core.Authentication;

import java.util.Objects;

/**
 * Default implementation of {@link AccountIdResolver} that looks in the connector config to find corresponding
 * AccountId definitions. If none is found, it returns a default account settings.
 */
public class DefaultAccountIdResolver implements BtpAccountIdResolver, IlpOverHttpAccountIdResolver, AccountIdResolver {

  @Override
  public AccountId resolveAccountId(final Link<?> link) {
    Objects.requireNonNull(link);

    if (link instanceof StatefulLink && ((StatefulLink<?>) link).isConnected() == false) {
      throw new LinkNotConnectedException("Disconnected Plugins do not have an associated account!", link.getLinkId());
    }
    //      if (link instanceof AbstractBtpPlugin) {
    //        // Connected Btp Plugins will have a BTP Session that can be used to get the accountId.
    //        final AbstractBtpPlugin abstractBtpPlugin = (AbstractBtpPlugin) link;
    //        return this.resolveAccountId(abstractBtpPlugin.getBtpSessionCredentials());
    //      }
    else {
      return AccountId.of(link.getLinkId().value());
    }
  }

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param btpSession The {@link BtpSession} to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied plugin.
   */
  @Override
  public AccountId resolveAccountId(final BtpSession btpSession) {
    Objects.requireNonNull(btpSession);

    return btpSession.getBtpSessionCredentials()
        .map(this::resolveAccountId)
        .orElseThrow(() -> new RuntimeException("No BtpSessionCredentials found!"));
  }

  /**
   * Determine the {@link AccountId} for the supplied plugin.
   *
   * @param btpSessionCredentials The {@link BtpSession} to introspect to determine the accountId that it represents.
   *
   * @return The {@link AccountId} for the supplied plugin.
   */
  @VisibleForTesting
  protected AccountId resolveAccountId(final BtpSessionCredentials btpSessionCredentials) {
    Objects.requireNonNull(btpSessionCredentials);

    return btpSessionCredentials.getAuthUsername()
        .map(AccountId::of)
        .orElseGet(() -> {
          // No AuthUserName, so get the AuthToken and hash it.
          //Route.HMAC(abstractBtpPlugin.getBtpSessionCredentials().getAuthToken());
          throw new RuntimeException("Not yet implemented!");
        });
  }

  @Override
  public AccountId resolveAccountId(final Authentication authentication) {
    Objects.requireNonNull(authentication);
    return AccountId.of(authentication.getPrincipal().toString());
  }
}
