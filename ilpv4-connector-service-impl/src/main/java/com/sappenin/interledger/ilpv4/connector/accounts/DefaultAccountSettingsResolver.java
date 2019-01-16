package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.settings.AccountSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Default implementation of {@link AccountSettingsResolver} that looks in the connector config to find an {@link
 * AccountSettings} object. If none is found, it returns a default account settings.
 *
 * TODO: Use Java SPI here so custom resolvers can be added.
 */
public class DefaultAccountSettingsResolver implements AccountSettingsResolver {
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public DefaultAccountSettingsResolver(final Supplier<ConnectorSettings> connectorSettingsSupplier) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
  }

  @Override
  public AccountSettings resolveAccountSettings(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    logger.debug("Resolving AccountSettings for AccountId: `{}`", accountId);
    return connectorSettingsSupplier.get().getAccountSettings().stream()
      .filter(accountSettings -> accountId.equals(accountSettings.getId()))
      .findFirst()
      // TODO: Need a default AccountSettings object?
      .orElseGet(() -> AccountSettings.builder().build());
  }
}
