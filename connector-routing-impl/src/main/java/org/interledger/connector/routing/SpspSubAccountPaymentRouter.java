package org.interledger.connector.routing;

import static org.interledger.connector.routing.Route.HMAC;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.sub.SpspSubAccountUtils;
import org.interledger.connector.accounts.sub.SubAccountUtils;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link PaymentRouter} that finds the next best-hop account for an Interledger address that
 * begins with the operator address of this connector. These types of addresses include ping packets as well as,
 * optionally, hosted SPSP-compatible sub-accounts.
 */
public class SpspSubAccountPaymentRouter implements PaymentRouter<Route> {

  private static final ImmutableRoute.Builder PING_ROUTE_BUILDER = Route.builder().nextHopAccountId(
    SubAccountUtils.PING_ACCOUNT_ID
  );

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final SpspSubAccountUtils spspSubAccountUtils;
  private final Decryptor decryptor;

  /**
   * Required-args Constructor.
   *
   * @param connectorSettingsSupplier A {@link Supplier} for this Connector's operating address.
   * @param spspSubAccountUtils       An {@link SpspSubAccountUtils} for loading account settings (will be removed as
   *                                  part of
   * @param decryptor                 A {@link Decryptor} for use in constructing Route authentication values.
   */
  public SpspSubAccountPaymentRouter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final SpspSubAccountUtils spspSubAccountUtils,
    final Decryptor decryptor
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.spspSubAccountUtils = Objects.requireNonNull(spspSubAccountUtils);
    this.decryptor = Objects.requireNonNull(decryptor);
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress, "finalDestinationAddress must not be null!");

    final EnabledProtocolSettings enabledProtocolSettings = this.connectorSettingsSupplier.get().enabledProtocols();

    /////////////////
    // Ping Protocol
    if (enabledProtocolSettings.isPingProtocolEnabled()) {
      // The SubAccount router will only ever be engaged for addresses that start with the Connector address. We
      // need one final check to see if there's an exact match, and only then utilize the ping protocol link.
      if (spspSubAccountUtils.isAddressForConnectorPingAccount(finalDestinationAddress)) {
        return Optional.of(
          PING_ROUTE_BUILDER.routePrefix(InterledgerAddressPrefix.of(finalDestinationAddress.getValue())).build()
        );
      }
    }

    // Decrypt the routingSecret, but only momentarily...
    final byte[] routingSecret = decryptor.decrypt(EncryptedSecret.fromEncodedValue(
      connectorSettingsSupplier.get().globalRoutingSettings().routingSecret()
    ));

    // If we get here, no need to check if this is a sub-account. We assume the caller of this method has already
    // done that.

    // Even though this is an SPSP request, we actually want to forward to the accountId of some user that this
    // request should go to. E.g., for a destination address of `g.connector.alice.123xyz`, the accountId that should
    // process this packet is `alice`.
    final AccountId accountId = spspSubAccountUtils.parseSpspAccountId(finalDestinationAddress);
    return Optional.of(ImmutableRoute.builder()
      .routePrefix(InterledgerAddressPrefix.of(finalDestinationAddress.getValue()))
      .nextHopAccountId(accountId)
      // No Path
      .auth(HMAC(routingSecret, InterledgerAddressPrefix.of(finalDestinationAddress.getValue())))
      .build()
    );
  }
}
