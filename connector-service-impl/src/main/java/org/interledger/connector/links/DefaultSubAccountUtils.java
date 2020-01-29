package org.interledger.connector.links;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.sub.SpspSubAccountUtils;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A Connector-wide default implementation of {@link SpspSubAccountUtils}.
 */
public class DefaultSubAccountUtils implements SpspSubAccountUtils {

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final InterledgerAddressPrefix spspAddressPrefix;

  /**
   * Required-args Constructor.
   *
   * @param connectorSettingsSupplier A {@link Supplier} for {@link ConnectorSettings} of this Connector.
   * @param spspAddressPrefix         The configured {@link InterledgerAddressPrefix} that the SPSP server is expected
   *                                  to use. Generally, this is `g.{connector}.{spsp-accountId}` as provisioned by an
   *                                  IL-DCP request from the SPSP server. However, this address could be manually
   *                                  provisioned in the SPSP server.
   */
  public DefaultSubAccountUtils(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final InterledgerAddressPrefix spspAddressPrefix
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.spspAddressPrefix = Objects.requireNonNull(spspAddressPrefix);
  }

  @Override
  public Supplier<InterledgerAddress> getConnectorOperatorAddress() {
    return () -> this.connectorSettingsSupplier.get().operatorAddress();
  }

  @Override
  public boolean shouldFulfilLocally(final InterledgerAddress destinationAddress) {
    Objects.requireNonNull(destinationAddress, "destinationAddress must not be null!");

    return connectorSettingsSupplier.get().enabledFeatures().isLocalSpspFulfillmentEnabled() && destinationAddress.startsWith(spspAddressPrefix);
  }

  @Override
  public AccountId parseSpspAccountId(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");

    //g.connector.spsp.bob.123xyz --> bob
    //g.connector.spsp.bob~foo.bar --> bob
    //g.connector.spsp.bob.~foo.bar --> bob
    //g.connector.spsp.bob~foo.bar --> bob~foo

    // Strip off spspAddress
    return AccountId.of(
      StringUtils.substringBefore(StringUtils.substringAfter(spspAddressPrefix.getValue(), "."), ".")
    );

  }
}
