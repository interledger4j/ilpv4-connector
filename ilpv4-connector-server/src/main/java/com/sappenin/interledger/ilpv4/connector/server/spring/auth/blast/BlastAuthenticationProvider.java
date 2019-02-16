package com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast;

import com.auth0.jwt.JWT;
import com.auth0.spring.security.api.JwtAuthenticationProvider;
import com.auth0.spring.security.api.authentication.JwtAuthentication;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.interledger.connector.link.blast.BlastHeaders.BLAST_AUDIENCE;

/**
 * An {@link AuthenticationProvider} that wraps an individual instance of {@link JwtAuthenticationProvider} for each
 * BLAST peer that authentication is required to be performed with.
 */
// TODO: Need to separate Auth for sendMoney and sendPacket....
public class BlastAuthenticationProvider implements AuthenticationProvider {

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final AccountManager accountManager;
  private final LoadingCache<AccountId, JwtAuthenticationProvider> jwtAuthenticationProviders;

  public BlastAuthenticationProvider(
    final Supplier<ConnectorSettings> connectorSettingsSupplier, final AccountManager accountManager
  ) {
    this.connectorSettingsSupplier = connectorSettingsSupplier;
    this.accountManager = Objects.requireNonNull(accountManager);

    // TODO: For a client-mode, we probably don't want to construct new instances of JwtVerifier, but for now we
    //  don't want to replicate all the logic of the underlying library, so we just use a cache to help reduce
    //  instantiations.
    jwtAuthenticationProviders = CacheBuilder.newBuilder()
      .maximumSize(100)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build(
        new CacheLoader<AccountId, JwtAuthenticationProvider>() {
          public JwtAuthenticationProvider load(final AccountId accountId) {
            Objects.requireNonNull(accountId);

            final byte[] secret = lookupBlastIncomingSecret(accountId).getBytes();
            final String issuer = accountId.value(); // The issuer is the sender of the token.
            return new JwtAuthenticationProvider(secret, issuer, BLAST_AUDIENCE);
          }
        });
  }

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    final String token = authentication.getCredentials().toString();

    return jwtAuthenticationProviders.getUnchecked(AccountId.of(JWT.decode(token).getIssuer()))
      .authenticate(authentication);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthentication.class.isAssignableFrom(authentication);
  }

  /**
   * Finds the BLAST shared secret for the specified accountId.
   *
   * TODO: Get this from vault instead...
   *
   * @param accountId
   *
   * @return
   */
  private String lookupBlastIncomingSecret(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    return accountManager.getAccount(accountId)
      .map(Account::getLink)
      .map(Link::getLinkSettings)
      .map(link -> (BlastLinkSettings) link)
      .map(BlastLinkSettings::getIncomingSecret)
      .orElseThrow(() -> new BadCredentialsException(
        String.format("No account found for `%s`", accountId.value())
      ));
  }
}
