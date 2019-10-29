package org.interledger.connector.accounts;

import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.link.Link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default implementation of {@link AccountSettingsResolver} that looks in the connector config to find an {@link
 * AccountSettings} object. If none is found, it returns a default account settings.
 */
public class DefaultAccountSettingsResolver implements AccountSettingsResolver {

  private final AccountSettingsRepository accountSettingsRepository;
  private final AccountIdResolver accountIdResolver;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public DefaultAccountSettingsResolver(
      final AccountSettingsRepository accountSettingsRepository,
      final AccountIdResolver accountIdResolver
  ) {
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
  }

  /**
   * Resolve account settings for the supplied {@code link}.
   *
   * @param link The {@link Link} to resolve Account Settings for.
   *
   * @return An {@link AccountSettings} resolved for the specified {@link Link}.
   */
  @Override
  public AccountSettings resolveAccountSettings(final Link<?> link) {
    Objects.requireNonNull(link);

    logger.debug("Resolving AccountSettings for Link: `{}`", link);

    final AccountId accountId = accountIdResolver.resolveAccountId(link);
    return this.accountSettingsRepository.findByAccountIdWithConversion(accountId)
        .orElseThrow(() -> new AccountNotFoundProblem(
            String.format("Unable to locate an AccountSettings for LinkId: `%s`", link.getLinkId()),
            accountId
        ));
  }
}
