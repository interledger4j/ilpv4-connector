package com.sappenin.interledger.ilpv4.connector.accounts;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.links.ping.PingProtocolLink;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link AccountSettingsResolver} that looks in the connector config to find an {@link
 * AccountSettings} object. If none is found, it returns a default account settings.
 *
 * TODO: Use Java SPI here so custom resolvers can be added.
 */
public class DefaultAccountSettingsResolver implements AccountSettingsResolver {
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final AccountIdResolver accountIdResolver;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public DefaultAccountSettingsResolver(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountIdResolver accountIdResolver
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
  }

  /**
   * For a given link, it will either be fully-defined in the ConnectorSettings as an "account", or it will be partially
   * defined in an AccountProvider.
   *
   * @param link The Link to resolve Account Settings for.
   *
   * @return
   */
  @Override
  public AccountSettings resolveAccountSettings(final Link<?> link) {
    Objects.requireNonNull(link);

    logger.debug("Resolving AccountSettings for Link: `{}`", link);

    final AccountId accountId = accountIdResolver.resolveAccountId(link);


    // 1. Just in-case, check the connector-settings to see if perhaps the connector is not yet connected.
    return connectorSettingsSupplier.get().getAccountSettings().stream()
      .filter(accountSettings -> accountSettings.getId().equals(accountId))
      .findFirst()
      .orElseGet(() -> {
        // If we get here, it means no account with the above identifier was pre-defined, so look in the
        // AccountProviders to find a dynamically generated instance.

        // 3. Check for any internally-routed links, such as Ping, etc.
        return this.resolveInternalAccountSettings(accountId, link)
          .orElseGet(() -> {

            // 4. Check for AccountProviders.
            /*
             * Implementation Note: What if we have a scenario where we have two Links of the same type, but we want some
             * connections to use AccountProvider1, and other connections to use AccountProvider2. In that world, we would
             * need some other signaling here to indicate such a thing. For example, we might set an attribute in the
             * Link that indicates the identifier of the accountProvider it should be using. This scenario is slightly
             * odd, however, because we would need two AccountProviders, each configured with a different AccountProvider
             * config and identifier, yet both serving requests on the same transport (like a BTP connection). So, in
             * order to pull this off, we would need pretty significant refactoring (or some other indicator in the
             * BTP or HTTP session to indicate the account-type).
             */
            return connectorSettingsSupplier.get().getAccountProviderSettings().stream()
              .filter(aps -> aps.getLinkType().equals(link.getLinkSettings().getLinkType()))
              .findFirst()
              .map(accountProviderSettings -> AccountSettings.from(accountProviderSettings).id(accountId).build())
              .orElseThrow(() -> new RuntimeException(String.format(
                "Unable to locate an AccountSettings for LinkId: `%s`", link.getLinkId())
              ));
          });
      });
  }

  /**
   * Produces default instances of {@link AccountSettings} for various internally-routed links. Any of these can be
   * overridden by specifying a link-type in the configuration properties.
   *
   * @param link
   *
   * @return
   */
  @VisibleForTesting
  protected Optional<AccountSettings> resolveInternalAccountSettings(final AccountId accountId, final Link<?> link) {
    if (PingProtocolLink.class.isAssignableFrom(link.getClass())) {
      // Return AccountSettings for the Ping Protocol.
      return Optional.of(AccountSettings.builder()
        .id(accountId)
        .linkType(PingProtocolLink.LINK_TYPE)
        .relationship(AccountRelationship.LOCAL)
        .description("PING protocol link")
        .maximumPacketAmount(BigInteger.ONE)
        .assetCode("USD")
        .assetScale(9)
        .build());
    } else {
      return Optional.empty();
    }

  }
}
