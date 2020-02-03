package org.interledger.connector.routing;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link PaymentRouter} that finds the next best-hop account for an Interledger address that
 * begins with the operator address of this connector. These types of addresses include ping packets as well as,
 * optionally, hosted SPSP-compatible sub-accounts.
 */
public class LocalDestinationAddressPaymentRouter implements PaymentRouter<Route> {

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final LocalDestinationAddressUtils localDestinationAddressUtils;
  private final Optional<AccountId> pingAccountId;

  /**
   * Required-args Constructor.
   *
   * @param connectorSettingsSupplier    A {@link Supplier} for this Connector's operating address.
   * @param localDestinationAddressUtils An {@link LocalDestinationAddressUtils} for loading account settings (will be
   *                                     removed as part of.
   */
  public LocalDestinationAddressPaymentRouter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final LocalDestinationAddressUtils localDestinationAddressUtils
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.localDestinationAddressUtils = Objects.requireNonNull(localDestinationAddressUtils);
    this.pingAccountId = Optional.of(localDestinationAddressUtils.getConnectorPingAccountId());
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress, "finalDestinationAddress must not be null!");

    final Optional<AccountId> accountId;
    if (localDestinationAddressUtils.isLocalDestinationAddress(finalDestinationAddress)) {
      /////////////////
      // Ping Protocol
      final EnabledProtocolSettings enabledProtocolSettings = this.connectorSettingsSupplier.get().enabledProtocols();
      if (enabledProtocolSettings.isPingProtocolEnabled()
        && localDestinationAddressUtils.isAddressForConnectorPingAccount(finalDestinationAddress)) {
        accountId = this.pingAccountId;
      } else if (localDestinationAddressUtils.isLocalSpspDestinationAddress(finalDestinationAddress)) {
        // Process SPSP
        accountId = Optional.of(localDestinationAddressUtils.parseSpspAccountId(finalDestinationAddress));
      } else if (localDestinationAddressUtils.isLocalAccountDestinationAddress(finalDestinationAddress)) {
        // Process Local Account
        accountId = Optional.of(localDestinationAddressUtils.parseLocalAccountId(finalDestinationAddress));
      } else {
        // The address _is_ a local destination address but didnt' match any known ILP address schema supported by
        // this Connector.
        accountId = Optional.empty();
      }
    } else {
      // The address is not a local destination address.
      accountId = Optional.empty();
    }

    return accountId
      .map($ -> ImmutableRoute.builder()
        .routePrefix(InterledgerAddressPrefix.of(finalDestinationAddress.getValue()))
        .nextHopAccountId($)
        // No Path since the Connector is generating the Route for itself.
        // No Auth since the Connector is generating the Route for itself.
        .build()
      );
  }
}
