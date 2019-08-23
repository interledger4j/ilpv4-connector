package org.interledger.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.EnabledProtocolSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.interledger.connector.routing.Route.HMAC;

/**
 * An implementation of {@link PaymentRouter} that finds the next best-hop account for an Interledger address that
 * begins with the operator address of this connector. These types of addresses include ping packets as well as,
 * optionally, hosted child accounts.
 */
public class ChildAccountPaymentRouter implements PaymentRouter<Route> {

  private static final ImmutableRoute.Builder PING_ROUTE_BUILDER =
    Route.builder().nextHopAccountId(PING_ACCOUNT_ID);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final LoadingCache<InterledgerAddress, Optional<Route>> childAccountRoutes;

  /**
   * Required-args Constructor.
   */
  public ChildAccountPaymentRouter(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountSettingsRepository accountSettingsRepository,
    final Decryptor decryptor
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);

    Objects.requireNonNull(accountSettingsRepository);
    Objects.requireNonNull(decryptor);

    this.childAccountRoutes = CacheBuilder.newBuilder()
      .maximumSize(5000) // TODO: Make size configurable
      // Expire after this duration, which will correspond to the last incoming request from the peer.
      .expireAfterAccess(30, TimeUnit.SECONDS) // TODO: Make this configurable
      .build(
        new CacheLoader<InterledgerAddress, Optional<Route>>() {

          /**
           * Given a {@code finalDestinationAddress}, find the child account that should be used for routing, and
           * return it. Currently this implementation is very simple, as it assumes the last segment of the ILP
           * address is the accountId.
           */
          public Optional<Route> load(final InterledgerAddress finalDestinationAddress) {
            Objects.requireNonNull(finalDestinationAddress);

            final AccountId accountId = parseChildAccountId(finalDestinationAddress);

            // Decrypt the routingSecret, but only momentarily...
            final byte[] routingSecret = decryptor.decrypt(EncryptedSecret.fromEncodedValue(
              connectorSettingsSupplier.get().getGlobalRoutingSettings().getRoutingSecret()
            ));

            try {
              return accountSettingsRepository.findByAccountId(accountId)
                .map(accountSettingsEntity ->
                  ImmutableRoute.builder()
                    .routePrefix(InterledgerAddressPrefix.of(finalDestinationAddress.getValue()))
                    .nextHopAccountId(accountId)
                    // No Path
                    .auth(HMAC(routingSecret, InterledgerAddressPrefix.of(finalDestinationAddress.getValue())))
                    .build()
                );
            } finally {
              // Zero-out all bytes in the `sharedSecretBytes` array.
              Arrays.fill(routingSecret, (byte) 0);
            }
          }
        });
  }

  /**
   * Given an {@link InterledgerAddress}, parse out the segment after the last period but before the Interactions
   * delimiter (`~`).
   *
   * @param interledgerAddress An {@link InterledgerAddress} to parse.
   *
   * @return An {@link AccountId} for the supplied address.
   */
  @VisibleForTesting
  AccountId parseChildAccountId(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "address must not be null!");

    //g.foo.bar~foo.bar
    return AccountId
      .of(StringUtils.substringAfterLast(StringUtils.substringBefore(interledgerAddress.getValue(), "~"), "."));
  }

  boolean isChildAccount(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");
    return interledgerAddress.startsWith(connectorSettingsSupplier.get().getOperatorAddressSafe());
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress, "finalDestinationAddress must not be null!");

    final EnabledProtocolSettings enabledProtocolSettings = this.connectorSettingsSupplier.get().getEnabledProtocols();

    /////////////////
    // Ping Protocol
    if (enabledProtocolSettings.isPingProtocolEnabled()) {
      // The ChildAccount router will only ever be engaged for addresses that start with the Connector address. We
      // need one final check to see if there's an exact match, and only then utilize the ping protocol link.
      if (connectorSettingsSupplier.get().getOperatorAddressSafe().equals(finalDestinationAddress)) {
        return Optional.of(
          PING_ROUTE_BUILDER
            .routePrefix(InterledgerAddressPrefix.of(finalDestinationAddress.getValue()))
            .build()
        );
      }
    }

    // Try child accounts.
    try {
      return this.childAccountRoutes.get(finalDestinationAddress);
    } catch (ExecutionException e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }
}
