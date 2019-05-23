package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.apache.commons.lang3.StringUtils;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.routing.Route.HMAC;

/**
 * An implementation of {@link PaymentRouter} that finds the next best-hop account for an Interledger address that
 * begins with the operator address of this connector.
 */
public class ChildAccountPaymentRouter implements PaymentRouter<Route> {

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
      // TODO: Make this configurable
      .maximumSize(5000)
      // Expire after this duration, which will correspond to the last incoming request from the peer.
      // TODO: Make this configurable
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .build(
        new CacheLoader<InterledgerAddress, Optional<Route>>() {

          /**
           * Given a {@code finalDestinationAddress}, find the child account that should be used for routing, and
           * return it. Currently this implementation is very simple, as it assumes the last segment of the ILP
           * address is the accountId.
           */
          public Optional<Route> load(final InterledgerAddress finalDestinationAddress) {
            Objects.requireNonNull(finalDestinationAddress);

            final AccountId accountId = parseAccountId(finalDestinationAddress);

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
  AccountId parseAccountId(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress);

    //g.foo.bar~foo.bar
    return AccountId
      .of(StringUtils.substringAfterLast(StringUtils.substringBefore(interledgerAddress.getValue(), "~"), "."));
  }

  public boolean isChildAccount(final InterledgerAddress interledgerAddress) {
    return interledgerAddress.startsWith(connectorSettingsSupplier.get().getOperatorAddressSafe());
  }

  @Override
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    Objects.requireNonNull(finalDestinationAddress);
    Preconditions.checkArgument(
      isChildAccount(connectorSettingsSupplier.get().getOperatorAddressSafe()),
      "This PaymentRouter only supports addresses that begin with the Operator address. Encountered: " +
        finalDestinationAddress
    );

    try {
      return this.childAccountRoutes.get(finalDestinationAddress);
    } catch (ExecutionException e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }
}
